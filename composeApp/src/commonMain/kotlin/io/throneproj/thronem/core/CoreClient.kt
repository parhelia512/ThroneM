package io.throneproj.thronem.core

import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.ConnectionEvents
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LogIterator
import io.nekohasekai.libbox.OpenConnectAuthResponse
import io.nekohasekai.libbox.OpenConnectStatusHandler
import io.nekohasekai.libbox.OpenConnectStatusSubscription
import io.nekohasekai.libbox.OpenConnectStatusUpdate
import io.nekohasekai.libbox.OpenVPNChallengeResponse
import io.nekohasekai.libbox.OpenVPNStatusHandler
import io.nekohasekai.libbox.OpenVPNStatusSubscription
import io.nekohasekai.libbox.OpenVPNStatusUpdate
import io.nekohasekai.libbox.OutboundGroup
import io.nekohasekai.libbox.OutboundGroupItem
import io.nekohasekai.libbox.OutboundGroupItemIterator
import io.nekohasekai.libbox.OutboundGroupIterator
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.libbox.StringIterator
import io.throneproj.thronem.bg.BackendState
import io.throneproj.thronem.core.ServiceEvent
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.toList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CoreRpcException(
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

data class CoreClashModeStatus(
    val modeList: List<String> = emptyList(),
    val currentMode: String = "",
)

/**
 * Typed suspend/Flow surface over the core. The implementation wraps a shared
 * libbox [CommandClient] bound to the in-process command server.
 */
interface CoreClient {
    fun subscribeServiceEvents(): Flow<ServiceEvent>
    fun subscribeStatus(): Flow<StatusMessage>
    fun subscribeLog(): Flow<CoreLogBatch>
    suspend fun clearLogs()
    fun subscribeConnections(): Flow<ConnectionEvents>
    suspend fun closeConnection(id: String)
    suspend fun closeAllConnections()
    fun subscribeGroups(): Flow<List<OutboundGroup>>
    fun subscribeOutbounds(): Flow<List<OutboundGroupItem>>
    suspend fun selectOutbound(groupTag: String, outboundTag: String)
    suspend fun setGroupExpand(groupTag: String, expand: Boolean)
    suspend fun getClashModeStatus(): CoreClashModeStatus
    fun subscribeClashMode(): Flow<CoreClashModeStatus>
    suspend fun setClashMode(mode: String)
    fun subscribeOpenConnectStatus(): Flow<OpenConnectStatusUpdate>
    suspend fun submitOpenConnectAuthResponse(
        endpointTag: String,
        challengeId: String,
        response: OpenConnectAuthResponse,
    )

    suspend fun cancelOpenConnectAuthChallenge(endpointTag: String, challengeId: String)
    fun subscribeOpenVPNStatus(): Flow<OpenVPNStatusUpdate>
    suspend fun submitOpenVPNChallengeResponse(
        endpointTag: String,
        challengeId: String,
        response: OpenVPNChallengeResponse,
    )

    suspend fun cancelOpenVPNChallenge(endpointTag: String, challengeId: String)

    /** Fire-and-forget delay refresh for a group member; results arrive via the groups stream. */
    suspend fun urlTest(outboundTag: String)

    /** Synchronous delay test against the running core; throws on failure. */
    suspend fun urlTest(tag: String, link: String, timeoutMs: Int): Int

    suspend fun checkConfig(config: String)
    suspend fun generateSchema(): String
    suspend fun resetNetwork()

    fun stunTest(server: String, outboundTag: String): Flow<STUNTestProgress>
    fun standaloneStunTest(server: String): Flow<STUNTestProgress>
    fun networkQualityTest(
        configUrl: String,
        outboundTag: String,
        serial: Boolean,
        maxRuntimeSeconds: Int,
        http3: Boolean,
    ): Flow<NetworkQualityTestProgress>

    fun standaloneNetworkQualityTest(
        configUrl: String,
        serial: Boolean,
        maxRuntimeSeconds: Int,
        http3: Boolean,
    ): Flow<NetworkQualityTestProgress>

    suspend fun close()
}

/** Events published by the service side of this process, consumed by the UI. */
object CoreEventBus {
    val events: MutableSharedFlow<ServiceEvent> = MutableSharedFlow(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
}

private class StreamHub<T>(buffer: Int = 64, replay: Int = 0) {
    val flow: SharedFlow<T> = MutableSharedFlow(
        replay = replay,
        extraBufferCapacity = buffer,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun emit(value: T) {
        @Suppress("UNCHECKED_CAST")
        (flow as MutableSharedFlow<T>).tryEmit(value)
    }
}

/**
 * libbox-backed [CoreClient]. One shared [CommandClient] subscribes to every
 * command stream; its handler callbacks fan out into hubs that the Flow
 * methods expose. A connect loop keeps the client attached across core
 * restarts (libbox does not reconnect on its own).
 */
open class LibboxCoreClient private constructor(
    private val statusIntervalMs: Long,
) : CoreClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val statusHub = StreamHub<StatusMessage>()
    private val logsHub = StreamHub<CoreLogBatch>()
    // The daemon's groups/outbounds streams are push-only: the initial snapshot is
    // sent once when the stream opens, right as the core flips to STARTED — before
    // the dashboard's collector subscribes. Keep the latest snapshot replayed so a
    // late collector still receives it (LxBox SPEC 015 §3.3, "empty main screen").
    private val groupsHub = StreamHub<List<OutboundGroup>>(replay = 1)
    private val outboundsHub = StreamHub<List<OutboundGroupItem>>(replay = 1)
    private val connectionsHub = StreamHub<ConnectionEvents>()
    private val clashModeHub = StreamHub<CoreClashModeStatus>()

    private val clashModeStatus = MutableStateFlow(CoreClashModeStatus())

    private val connected = MutableStateFlow(false)

    private val client: CommandClient = run {
        val options = CommandClientOptions().apply {
            setStatusInterval(statusIntervalMs)
            addCommand(Libbox.CommandStatus)
            addCommand(Libbox.CommandGroup)
            addCommand(Libbox.CommandOutbounds)
            addCommand(Libbox.CommandConnections)
            addCommand(Libbox.CommandLog)
            addCommand(Libbox.CommandClashMode)
        }
        Libbox.newCommandClient(Handler(), options)
    }

    init {
        scope.launch { connectLoop() }
    }

    private suspend fun connectLoop() {
        var backoffMs = 500L
        while (scope.isActive) {
            if (connected.value) {
                backoffMs = 500L
                delay(1000)
                continue
            }
            try {
                client.connect()
                // connected is flipped by Handler.connected(); avoid a hot loop
                // when the server refuses us without ever calling it.
                if (!connected.value) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(5000)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(5000)
            }
        }
    }

    private inner class Handler : CommandClientHandler {
        override fun clearLogs() {
            logsHub.emit(CoreLogBatch(reset = true, entries = emptyList()))
        }

        override fun connected() {
            connected.value = true
            // The CommandServer is alive from Application.onCreate onward, so this
            // callback fires on every reconnect regardless of whether a proxy box
            // is running. Pulling groups/outbounds while no box is STARTED just
            // produces Unimplemented/not-STARTED errors, so gate the pull on the
            // service-side state machine (Connecting/Connected both report
            // started = true). The push streams still deliver the initial
            // snapshot when the box actually boots, so a skipped pull here does
            // not lose data (LxBox SPEC 015 §3.3, §122).
            if (!BackendState.status.value.state.started) return
            // Pull snapshots via the lx_command unary getters (requires
            // with_lx_command, present in the bundled AAR). The push streams
            // deliver their initial snapshot exactly once at stream open; if
            // that raced ahead of a collector (or the stream broke), a pull is
            // the only way to re-read the group tree (LxBox §122). Best-effort:
            // a missing getter is not an error, the streams still deliver.
            scope.launch {
                runCatching { emitGroups(client.getGroups()) }
                    .onFailure { Logs.d("pull groups: ${it.message}") }
                runCatching { emitOutbounds(client.getOutbounds()) }
                    .onFailure { Logs.d("pull outbounds: ${it.message}") }
            }
        }

        override fun disconnected(message: String?) {
            connected.value = false
        }

        override fun initializeClashMode(modeList: StringIterator?, currentMode: String?) {
            clashModeStatus.value = CoreClashModeStatus(
                modeList = modeList?.toList() ?: emptyList(),
                currentMode = currentMode ?: "",
            )
            clashModeHub.emit(clashModeStatus.value)
        }

        override fun updateClashMode(currentMode: String?) {
            clashModeStatus.value = clashModeStatus.value.copy(currentMode = currentMode.orEmpty())
            clashModeHub.emit(clashModeStatus.value)
        }

        override fun writeConnectionEvents(events: ConnectionEvents?) {
            if (events != null) connectionsHub.emit(events)
        }

        override fun writeDNSQuery(query: io.nekohasekai.libbox.DnsQuery?) {
            // The DNS command stream is not subscribed.
        }

        private fun emitGroups(groups: OutboundGroupIterator) {
            groupsHub.emit(buildList {
                while (groups.hasNext()) add(groups.next())
            })
        }

        private fun emitOutbounds(outbounds: OutboundGroupItemIterator) {
            outboundsHub.emit(buildList {
                while (outbounds.hasNext()) add(outbounds.next())
            })
        }

        override fun writeGroups(groups: OutboundGroupIterator?) {
            if (groups != null) emitGroups(groups)
        }

        override fun writeLogs(logs: LogIterator?) {
            if (logs == null) return
            val entries = ArrayList<CoreLogEntry>(logs.len())
            while (logs.hasNext()) {
                val entry = logs.next()
                entries.add(
                    CoreLogEntry(
                        level = CoreLogLevel.forNumber(entry.level) ?: CoreLogLevel.INFO,
                        message = entry.message,
                    ),
                )
            }
            logsHub.emit(CoreLogBatch(reset = false, entries = entries))
        }

        override fun writeOutbounds(outbounds: OutboundGroupItemIterator?) {
            if (outbounds != null) emitOutbounds(outbounds)
        }

        override fun writeStatus(status: StatusMessage?) {
            if (status != null) statusHub.emit(status)
        }

        override fun setDefaultLogLevel(level: Int) {
            // No-op: log level is controlled directly via the CommandServer.
        }
    }

    private suspend fun requireConnected(): CommandClient {
        if (!connected.value) {
            try {
                client.connect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw CoreRpcException("Unavailable", e.message ?: "core client is not connected", e)
            }
        }
        return client
    }

    override fun subscribeServiceEvents(): Flow<ServiceEvent> = CoreEventBus.events

    override fun subscribeStatus(): Flow<StatusMessage> = statusHub.flow

    override fun subscribeLog(): Flow<CoreLogBatch> = logsHub.flow

    override suspend fun clearLogs() {
        requireConnected().clearLogs()
    }

    override fun subscribeConnections(): Flow<ConnectionEvents> = connectionsHub.flow

    override suspend fun closeConnection(id: String) {
        requireConnected().closeConnection(id)
    }

    override suspend fun closeAllConnections() {
        requireConnected().closeConnections()
    }

    override fun subscribeGroups(): Flow<List<OutboundGroup>> = groupsHub.flow

    override fun subscribeOutbounds(): Flow<List<OutboundGroupItem>> = outboundsHub.flow

    override suspend fun selectOutbound(groupTag: String, outboundTag: String) {
        requireConnected().selectOutbound(groupTag, outboundTag)
    }

    override suspend fun setGroupExpand(groupTag: String, expand: Boolean) {
        requireConnected().setGroupExpand(groupTag, expand)
    }

    override suspend fun getClashModeStatus(): CoreClashModeStatus = clashModeStatus.value

    override fun subscribeClashMode(): Flow<CoreClashModeStatus> = clashModeHub.flow

    override suspend fun setClashMode(mode: String) {
        requireConnected().setClashMode(mode)
    }

    override fun subscribeOpenConnectStatus(): Flow<OpenConnectStatusUpdate> = channelFlow {
        // The core rejects subscriptions with "invalid argument" until a box
        // is running; retry with backoff instead of giving up for good.
        var backoffMs = 500L
        while (isActive) {
            var session: OpenConnectStatusSubscription? = null
            val closed = CompletableDeferred<String?>()
            try {
                session = requireConnected().subscribeOpenConnectStatus(object : OpenConnectStatusHandler {
                    override fun onError(message: String?) {
                        closed.complete(message)
                    }

                    override fun onStatusUpdate(update: OpenConnectStatusUpdate?) {
                        if (update != null) trySend(update)
                    }
                })
                backoffMs = 500L
                val message = closed.await()
                if (!message.isNullOrEmpty()) {
                    Logs.w("subscribe openconnect status: $message")
                }
            } catch (e: CancellationException) {
                runCatching { session?.close() }
                throw e
            } catch (e: Exception) {
                Logs.w("subscribe openconnect status", e)
            }
            runCatching { session?.close() }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(5000L)
        }
    }

    override suspend fun submitOpenConnectAuthResponse(
        endpointTag: String,
        challengeId: String,
        response: OpenConnectAuthResponse,
    ) {
        requireConnected().submitOpenConnectAuthResponse(endpointTag, challengeId, response)
    }

    override suspend fun cancelOpenConnectAuthChallenge(endpointTag: String, challengeId: String) {
        requireConnected().cancelOpenConnectAuthChallenge(endpointTag, challengeId)
    }

    override fun subscribeOpenVPNStatus(): Flow<OpenVPNStatusUpdate> = channelFlow {
        var backoffMs = 500L
        while (isActive) {
            var session: OpenVPNStatusSubscription? = null
            val closed = CompletableDeferred<String?>()
            try {
                session = requireConnected().subscribeOpenVPNStatus(object : OpenVPNStatusHandler {
                    override fun onError(message: String?) {
                        closed.complete(message)
                    }

                    override fun onStatusUpdate(update: OpenVPNStatusUpdate?) {
                        if (update != null) trySend(update)
                    }
                })
                backoffMs = 500L
                val message = closed.await()
                if (!message.isNullOrEmpty()) {
                    Logs.w("subscribe openvpn status: $message")
                }
            } catch (e: CancellationException) {
                runCatching { session?.close() }
                throw e
            } catch (e: Exception) {
                Logs.w("subscribe openvpn status", e)
            }
            runCatching { session?.close() }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(5000L)
        }
    }

    override suspend fun submitOpenVPNChallengeResponse(
        endpointTag: String,
        challengeId: String,
        response: OpenVPNChallengeResponse,
    ) {
        requireConnected().submitOpenVPNChallengeResponse(endpointTag, challengeId, response)
    }

    override suspend fun cancelOpenVPNChallenge(endpointTag: String, challengeId: String) {
        requireConnected().cancelOpenVPNChallenge(endpointTag, challengeId)
    }

    override suspend fun urlTest(outboundTag: String) {
        requireConnected().urlTest(outboundTag)
    }

    override suspend fun urlTest(tag: String, link: String, timeoutMs: Int): Int {
        val result = requireConnected().urlTestOutbound(tag, link, timeoutMs)
        if (!result.error.isNullOrEmpty()) {
            throw CoreRpcException("Unknown", result.error)
        }
        return result.delay
    }

    override suspend fun checkConfig(config: String) {
        try {
            Libbox.checkConfig(config)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw CoreRpcException("InvalidArgument", e.message ?: "invalid config", e)
        }
    }

    override suspend fun generateSchema(): String = Libbox.generateConfigSchema().value

    override suspend fun resetNetwork() {
        resetCoreNetwork()
    }

    override fun stunTest(server: String, outboundTag: String): Flow<STUNTestProgress> =
        callbackFlow {
            val session = requireConnected().startSTUNTest(
                server,
                outboundTag,
                object : io.nekohasekai.libbox.STUNTestHandler {
                    override fun onError(message: String?) {
                        trySend(STUNTestProgress(error = message, isFinal = true))
                    }

                    override fun onProgress(progress: io.nekohasekai.libbox.STUNTestProgress?) {
                        if (progress != null) trySend(STUNTestProgress(progress))
                    }

                    override fun onResult(result: io.nekohasekai.libbox.STUNTestResult?) {
                        if (result != null) {
                            trySend(STUNTestProgress(result))
                            close()
                        }
                    }
                },
            )
            awaitClose {
                runCatching { session.close() }
            }
        }

    override fun standaloneStunTest(server: String): Flow<STUNTestProgress> = callbackFlow {
        val test = Libbox.newSTUNTest()
        test.start(
            server,
            object : io.nekohasekai.libbox.STUNTestHandler {
                override fun onError(message: String?) {
                    trySend(STUNTestProgress(error = message, isFinal = true))
                }

                override fun onProgress(progress: io.nekohasekai.libbox.STUNTestProgress?) {
                    if (progress != null) trySend(STUNTestProgress(progress))
                }

                override fun onResult(result: io.nekohasekai.libbox.STUNTestResult?) {
                    if (result != null) {
                        trySend(STUNTestProgress(result))
                        close()
                    }
                }
            },
        )
        awaitClose {
            test.cancel()
        }
    }

    override fun networkQualityTest(
        configUrl: String,
        outboundTag: String,
        serial: Boolean,
        maxRuntimeSeconds: Int,
        http3: Boolean,
    ): Flow<NetworkQualityTestProgress> = callbackFlow {
        val session = requireConnected().startNetworkQualityTest(
            configUrl,
            outboundTag,
            serial,
            maxRuntimeSeconds,
            http3,
            object : io.nekohasekai.libbox.NetworkQualityTestHandler {
                override fun onError(message: String?) {
                    trySend(NetworkQualityTestProgress(error = message, isFinal = true))
                }

                override fun onProgress(progress: io.nekohasekai.libbox.NetworkQualityProgress?) {
                    if (progress != null) trySend(NetworkQualityTestProgress(progress))
                }

                override fun onResult(result: io.nekohasekai.libbox.NetworkQualityResult?) {
                    if (result != null) {
                        trySend(NetworkQualityTestProgress(result))
                        close()
                    }
                }
            },
        )
        awaitClose {
            runCatching { session.close() }
        }
    }

    override fun standaloneNetworkQualityTest(
        configUrl: String,
        serial: Boolean,
        maxRuntimeSeconds: Int,
        http3: Boolean,
    ): Flow<NetworkQualityTestProgress> = callbackFlow {
        val test = Libbox.newNetworkQualityTest()
        test.start(
            configUrl,
            serial,
            maxRuntimeSeconds,
            http3,
            object : io.nekohasekai.libbox.NetworkQualityTestHandler {
                override fun onError(message: String?) {
                    trySend(NetworkQualityTestProgress(error = message, isFinal = true))
                }

                override fun onProgress(progress: io.nekohasekai.libbox.NetworkQualityProgress?) {
                    if (progress != null) trySend(NetworkQualityTestProgress(progress))
                }

                override fun onResult(result: io.nekohasekai.libbox.NetworkQualityResult?) {
                    if (result != null) {
                        trySend(NetworkQualityTestProgress(result))
                        close()
                    }
                }
            },
        )
        awaitClose {
            test.cancel()
        }
    }

    override suspend fun close() {
        client.disconnect()
    }

    companion object {

        /** Local client bound to the in-process command server. */
        fun local(): LibboxCoreClient = LibboxCoreClient(
            statusIntervalMs = DataStore.speedInterval.getBlocking().coerceAtLeast(100).toLong(),
        )

        fun buildEnvironment(): String {
            return "android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}), ${android.os.Build.MODEL}"
        }
    }
}

/** Platform hook: resets the core network state (CommandServer.resetNetwork on Android). */
expect fun resetCoreNetwork()
