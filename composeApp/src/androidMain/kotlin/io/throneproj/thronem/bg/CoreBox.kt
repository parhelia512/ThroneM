package io.throneproj.thronem.bg

import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.SystemProxyStatus
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.toStringIterator

/**
 * Owner of the in-process libbox [CommandServer]. Created once when the
 * application starts; services drive the actual box instance through
 * [startService]/[stopService], and the UI reads state via the command client
 * exposed through Koin.
 */
object CoreBox {

    @Volatile
    private var commandServer: CommandServer? = null

    fun start() {
        if (commandServer != null) return
        val server = Libbox.newCommandServer(Handler(), AndroidPlatformInterface())
        server.start()
        commandServer = server
    }

    fun close() {
        runCatching { commandServer?.close() }
        commandServer = null
    }

    /**
     * Starts (or hot-reloads) the box with [configJson]. Blocking while the
     * core boots — call from a background dispatcher.
     */
    fun startService(configJson: String) {
        val server = commandServer ?: error("command server is not started")
        // libbox dereferences the options struct unconditionally — a null here
        // panics the Go runtime and kills the whole process.
        val options = OverrideOptions().apply {
            autoRedirect = false
            includePackage = emptyList<String>().toStringIterator(0)
            excludePackage = emptyList<String>().toStringIterator(0)
        }
        server.startOrReloadService(configJson, options)
    }

    fun stopService() {
        runCatching { commandServer?.closeService() }
            .onFailure { Logs.w("stopService", it) }
    }

    fun hasInstance(): Boolean = commandServer?.ready() == true

    fun resetNetwork() {
        runCatching { commandServer?.resetNetwork() }
            .onFailure { Logs.w("resetNetwork", it) }
    }

    fun pause() {
        runCatching { commandServer?.pause() }
            .onFailure { Logs.w("pause", it) }
    }

    fun wake() {
        runCatching { commandServer?.wake() }
            .onFailure { Logs.w("wake", it) }
    }

    fun needWIFIState(): Boolean = commandServer?.needWIFIState() == true

    fun updateWIFIState() {
        runCatching { commandServer?.updateWIFIState() }
    }

    private class Handler : io.nekohasekai.libbox.CommandServerHandler {

        override fun connectSSHAgent(): Int {
            throw UnsupportedOperationException("SSH agent is not supported on Android")
        }

        override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus().apply {
            setAvailable(false)
        }

        override fun serviceReload() {
            ServiceRegistry.baseService?.reload()
        }

        override fun serviceStop() {
            // The core stopped on its own (fatal or external stop). If the
            // runner already initiated the stop, this call is ignored by the
            // state check inside stopRunner.
            ServiceRegistry.baseService?.stopRunner()
        }

        override fun setSystemProxyEnabled(enabled: Boolean) {
            // Android has no global system proxy.
        }

        override fun triggerNativeCrash() {
            // Debug tool without a native counterpart here.
        }

        override fun writeDebugMessage(message: String?) {
            Logs.d(message.orEmpty())
        }
    }
}
