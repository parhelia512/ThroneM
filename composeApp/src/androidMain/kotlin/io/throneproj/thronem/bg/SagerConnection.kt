package io.throneproj.thronem.bg

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.throneproj.thronem.Action
import io.throneproj.thronem.Key
import io.throneproj.thronem.database.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Lifecycle-only binder keeping the foreground service alive. State / speed /
 * alerts travel through CoreEventBus in-process; this connection exists so
 * BIND_AUTO_CREATE keeps the service running.
 */
class SagerConnection(
    private var listenForDeath: Boolean = false,
    private val mirror: ServiceEventMirror = GlobalContext.get().get(),
) : ServiceConnection, IBinder.DeathRecipient {

    companion object {
        val serviceClass
            get() = when (DataStore.serviceMode.getBlocking()) {
                Key.MODE_PROXY -> ProxyService::class
                Key.MODE_VPN -> VpnService::class
                else -> throw UnknownError()
            }.java
    }

    private var connectionActive = false
    private var appContext: Context? = null
    private var reconnectAttempted = false
    private var binder: IBinder? = null
    private var scope: CoroutineScope? = null
    private var mirrorJob: Job? = null

    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        this.binder = binder
        if (listenForDeath) binder.linkToDeath(this, 0)
        cancelMirror()
        val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = connectionScope
        mirrorJob = connectionScope.launch { mirror.events.collect() }
        BackendState.setConnected(true)
        restoreServiceState()
        reconnectAttempted = false
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        BackendState.setConnected(false)
        cancelMirror()
        binder = null
        resetStatus()
        tryReconnect()
    }

    override fun binderDied() {
        BackendState.setConnected(false)
        cancelMirror()
        binder = null
        resetStatus()
        tryReconnect()
    }

    fun connect(context: Context) {
        appContext = context.applicationContext
        reconnectAttempted = false
        if (connectionActive && binder != null) return
        connectionActive = true
        val intent = Intent(appContext, serviceClass).setAction(Action.SERVICE)
        appContext!!.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun disconnect(context: Context) {
        if (connectionActive) try {
            context.unbindService(this)
        } catch (_: IllegalArgumentException) {
        }
        connectionActive = false
        reconnectAttempted = false
        if (listenForDeath) try {
            binder?.unlinkToDeath(this, 0)
        } catch (_: NoSuchElementException) {
        }
        cancelMirror()
        binder = null
        BackendState.setConnected(false)
    }

    fun reconnect(context: Context) {
        disconnect(context)
        connect(context)
    }

    private fun cancelMirror() {
        mirrorJob?.cancel()
        mirrorJob = null
        scope?.cancel()
        scope = null
    }

    private fun resetStatus() {
        DataStore.serviceState = ServiceState.Idle
        BackendState.reset()
    }

    /**
     * Re-publish the running service's live state for UI subscribers that
     * missed the state-change event: a rebind after activity recreation
     * carries no state event and CoreEventBus does not replay, so without
     * this the connect button would read Idle while the proxy keeps running.
     */
    private fun restoreServiceState() {
        val data = ServiceRegistry.baseService?.data ?: return
        DataStore.serviceState = data.state
        BackendState.updateState(data.state, data.proxy?.displayProfileName)
    }

    private fun tryReconnect() {
        val appContext = appContext ?: return
        if (!connectionActive || binder != null || reconnectAttempted) return
        reconnectAttempted = true
        val intent = Intent(appContext, serviceClass).setAction(Action.SERVICE)
        appContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }
}
