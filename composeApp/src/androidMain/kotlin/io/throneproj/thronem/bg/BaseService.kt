package io.throneproj.thronem.bg

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.getSystemService
import io.throneproj.thronem.Action
import io.throneproj.thronem.bg.proto.ProxyInstance
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.broadcastReceiver
import io.throneproj.thronem.ktx.hasPermission
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.ktx.runOnDefaultDispatcher
import io.throneproj.thronem.ktx.runOnMainDispatcher
import io.throneproj.thronem.ktx.showToast
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.milliseconds

class BaseService {

    interface BackendEngine {
        var proxy: ProxyInstance?

        suspend fun init(profile: ProxyEntity)

        suspend fun start(onFatal: suspend (Throwable) -> Unit)

        fun stop()

        fun resetNetwork()
    }

    private class AndroidBackendEngine(private val service: Interface) : BackendEngine {
        override var proxy: ProxyInstance? = null

        override suspend fun init(profile: ProxyEntity) {
            proxy = ProxyInstance(profile, service).also {
                it.init(service is VpnService)
            }
        }

        override suspend fun start(onFatal: suspend (Throwable) -> Unit) {
            val proxy = proxy ?: return
            proxy.launch()
        }

        override fun stop() {
            proxy?.close()
            proxy = null
        }

        override fun resetNetwork() {
            val proxy = proxy
            if (proxy != null && proxy.isInitialized() && CoreBox.hasInstance()) {
                runCatching {
                    CoreBox.resetNetwork()
                }
            }
        }
    }

    class Data internal constructor(val service: Interface) {
        var state = ServiceState.Stopped
        val backend = service.createBackendEngine()
        var proxy: ProxyInstance?
            get() = backend.proxy
            set(value) {
                backend.proxy = value
            }
        var notification: ServiceNotifier = NoopServiceNotifier

        val receiver = broadcastReceiver { ctx, intent ->
            when (intent.action) {
                Action.RELOAD -> service.reload()
                // Action.SWITCH_WAKE_LOCK -> runOnDefaultDispatcher { service.switchWakeLock() }
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    val powerManager = (service as Context).getSystemService<PowerManager>()!!
                    val proxy = proxy
                    if (proxy != null && proxy.isInitialized() && CoreBox.hasInstance()) {
                        if (powerManager.isDeviceIdleMode) {
                            CoreBox.pause()
                        } else {
                            CoreBox.wake()
                        }
                    }
                }

                Action.RESET_UPSTREAM_CONNECTIONS -> runOnDefaultDispatcher {
                    withTimeoutOrNull(1000.milliseconds) {
                        resetNetwork()
                        withContext(Dispatchers.Main) {
                            collapseStatusBar(ctx)
                            showToast(resolveRepository().getString(Res.string.have_reset_network))
                        }
                    }
                }

                else -> service.stopRunner()
            }
        }

        @SuppressLint("WrongConstant")
        private fun collapseStatusBar(context: Context) {
            try {
                val statusBarManager = context.getSystemService("statusbar")
                val collapse = statusBarManager.javaClass.getMethod("collapsePanels")
                collapse.invoke(statusBarManager)
            } catch (_: Exception) {
            }
        }

        var closeReceiverRegistered = false

        /** Lifecycle-only binder so BIND_AUTO_CREATE keeps the foreground service alive. */
        val binder = Binder()
        var connectingJob: Job? = null

        fun changeState(s: ServiceState, message: String? = null) {
            if (state == s && message == null) return
            state = s
            DataStore.serviceState = s
            BackendState.updateState(s, proxy?.displayProfileName)
            ServiceEventPublisher.publishState(s, proxy?.displayProfileName)
        }

        fun resetNetwork() {
            backend.resetNetwork()
        }
    }

    interface Interface {
        val data: Data
        val tag: String
        fun createBackendEngine(): BackendEngine = AndroidBackendEngine(this)
        fun createNotifier(profileName: String): ServiceNotifier = NoopServiceNotifier

        fun onBind(intent: Intent): IBinder? = if (intent.action == Action.SERVICE) {
            data.binder
        } else {
            null
        }

        fun reload() {
            if (DataStore.selectedProxySet.getBlocking() == 0L) {
                stopRunner(false, runBlocking { resolveRepository().getString(Res.string.profile_empty) })
                return
            }

            val state = data.state
            val restartCurrentService = javaClass == SagerConnection.serviceClass
            when {
                state == ServiceState.Stopped -> {
                    if (restartCurrentService) {
                        startRunner()
                    } else {
                        resolveRepository().startService()
                    }
                }

                state.canStop -> {
                    if (restartCurrentService) {
                        stopRunner(true)
                    } else {
                        stopRunner(afterStop = resolveRepository()::startService)
                    }
                }

                else -> Logs.w("Illegal state $state when invoking use")
            }
        }

        suspend fun startProcesses() {
            data.backend.start { throwable ->
                stopRunner(false, throwable.readableMessage)
            }
            if (CoreBox.needWIFIState()) {
                val wifiPermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_FINE_LOCATION
                } else {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }
                this as Context
                if (!hasPermission(wifiPermission)) {
                    ServiceEventPublisher.publishAlert(ServiceAlert.NeedWifiPermission)
                }
            }
        }

        fun startRunner() {
            this as Context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, javaClass))
            } else {
                startService(Intent(this, javaClass))
            }
        }

        fun killProcesses() {
            data.backend.stop()
            wakeLock?.apply {
                release()
                wakeLock = null
            }
            runOnDefaultDispatcher {
                DefaultNetworkMonitor.stop()
            }
        }

        fun stopRunner(
            restart: Boolean = false,
            msg: String? = null,
            afterStop: (() -> Unit)? = null,
        ) {
            ServiceRegistry.baseService = null
            ServiceRegistry.vpnService = null

            if (data.state == ServiceState.Stopping) return
            this as Service
            OpenConnectAuthWatcher.stop(this)
            OpenVPNAuthWatcher.stop(this)
            data.notification.destroy()
            data.notification = NoopServiceNotifier

            data.changeState(ServiceState.Stopping)

            runOnMainDispatcher {
                data.connectingJob?.cancelAndJoin() // ensure stop connecting first
                // we use a coroutineScope here to allow clean-up in parallel
                coroutineScope {
                    killProcesses()
                    val data = data
                    if (data.closeReceiverRegistered) {
                        unregisterReceiver(data.receiver)
                        data.closeReceiverRegistered = false
                    }
                }

                // change the state
                data.changeState(ServiceState.Stopped, msg)
                if (!msg.isNullOrBlank()) {
                    ServiceEventPublisher.publishAlert(ServiceAlert.Common(msg))
                }
                // stop the service if nothing has bound to it
                if (restart) startRunner() else {
                    afterStop?.invoke()
                    stopSelf()
                }
            }
        }

        // networks
        var upstreamInterfaceName: String?

        suspend fun preInit() {
            DefaultNetworkMonitor.start()
        }

        var wakeLock: PowerManager.WakeLock?
        fun acquireWakeLock()

        suspend fun lateInit() {
            wakeLock?.apply {
                release()
                wakeLock = null
            }

            if (DataStore.acquireWakeLock.get()) {
                acquireWakeLock()
                data.notification.onWakeLock(true)
            } else {
                data.notification.onWakeLock(false)
            }
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            ServiceRegistry.baseService = this

            val data = data
            if (data.state != ServiceState.Stopped) return Service.START_NOT_STICKY
            data.notification = createNotifier("")
            // The service root is the proxy set selected on the proxy sets page,
            // synthesized into an in-memory entity for the config builder.
            val profile = runBlocking {
                val selectedSet = ThroneDatabase.proxySetDao
                    .getById(DataStore.selectedProxySet.getBlocking())
                    .firstOrNull()
                selectedSet?.bean?.let { bean ->
                    ProxyEntity().apply {
                        id = selectedSet.id
                        putBean(bean)
                    }
                }
            }
            this as Context
            if (profile == null) { // gracefully shutdown: https://stackoverflow.com/q/47337857/2245107
                stopRunner(false, runBlocking { resolveRepository().getString(Res.string.profile_empty) })
                return Service.START_NOT_STICKY
            }

            setBootReceiverEnabled(DataStore.persistAcrossReboot.getBlocking())
            if (!data.closeReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(Action.RELOAD)
                    addAction(Intent.ACTION_SHUTDOWN)
                    addAction(Action.CLOSE)
                    // addAction(Action.SWITCH_WAKE_LOCK)
                    addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
                    addAction(Action.RESET_UPSTREAM_CONNECTIONS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(
                        data.receiver,
                        filter,
                        "$packageName.permission.SERVICE",
                        null,
                        Context.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    registerReceiver(
                        data.receiver,
                        filter,
                        "$packageName.permission.SERVICE",
                        null,
                    )
                }
                data.closeReceiverRegistered = true
            }

            data.changeState(ServiceState.Connecting)
            data.connectingJob = runOnDefaultDispatcher {
                try {
                    data.notification.onTitle(profile.displayNameForService())

                    preInit()
                    data.backend.init(profile)
                    DataStore.currentProfile.set(profile.id)

                    startProcesses()
                    data.changeState(ServiceState.Connected)
                    OpenConnectAuthWatcher.start(this@Interface)
                    OpenVPNAuthWatcher.start(this@Interface)

                    lateInit()
                } catch (_: CancellationException) { // if the job was cancelled, it is canceller's responsibility to call stopRunner
                } catch (e: UnknownHostException) {
                    Logs.e(e)
                    stopRunner(false, resolveRepository().getString(Res.string.invalid_server))
                } catch (exc: Throwable) {
                    Logs.e(exc.readableMessage)
                    stopRunner(
                        false,
                        "${resolveRepository().getString(Res.string.service_failed)}: ${exc.readableMessage}",
                    )
                } finally {
                    data.connectingJob = null
                }
            }
            return Service.START_NOT_STICKY
        }
    }

}
