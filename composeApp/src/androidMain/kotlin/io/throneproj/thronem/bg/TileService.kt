package io.throneproj.thronem.bg

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.core.content.ContextCompat
import io.throneproj.thronem.Key
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.lib.R
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.app_name
import io.throneproj.thronem.ui.VpnRequestActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.net.VpnService as BaseVpnService
import android.service.quicksettings.TileService as BaseTileService

class TileService : BaseTileService() {
    private val iconRest by lazy { Icon.createWithResource(this, R.drawable.ic_service_rest) }
    private val iconConnected by lazy {
        Icon.createWithResource(this, R.drawable.ic_throne_tile)
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var listenJob: Job? = null

    override fun attachBaseContext(newBase: Context) {
        val languageContext = ContextCompat.getContextForLanguage(newBase)
        super.attachBaseContext(languageContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
        // The tile runs in the same process as the services, so the state written by
        // BaseService.changeState is readable right here.
        listenJob = scope.launch {
            BackendState.status.collect { status ->
                updateTile(status.state, status.profileName)
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        if (isLocked) unlockAndRun(this::toggle) else toggle()
    }

    private fun updateTile(serviceState: ServiceState, profileName: String?) {
        qsTile?.apply {
            label = null
            when (serviceState) {
                ServiceState.Connecting -> {
                    icon = iconRest
                    state = Tile.STATE_ACTIVE
                }

                ServiceState.Connected -> {
                    icon = iconConnected
                    label = profileName
                    state = Tile.STATE_ACTIVE
                }

                ServiceState.Stopping -> {
                    icon = iconRest
                    state = Tile.STATE_UNAVAILABLE
                }

                // Stopped
                else -> {
                    icon = iconRest
                    state = Tile.STATE_INACTIVE
                }
            }
            label = label ?: runBlocking {
                resolveRepository().getString(Res.string.app_name)
            }
            updateTile()
        }
    }

    private fun toggle() {
        scope.launch {
            val state = DataStore.serviceState
            when {
                state.canStop -> {
                    updateTile(ServiceState.Stopping, null)
                    resolveRepository().stopService()
                }

                state == ServiceState.Stopped || state == ServiceState.Idle -> {
                    updateTile(ServiceState.Connecting, null)
                    startServiceFromTile()
                }
            }
        }
    }

    /**
     * [Android 15: TileService onClick does not allow startForegroundService](https://issuetracker.google.com/issues/377528724)
     *
     * Inspired by: [WireGuard Android QuickTileService.kt](https://github.com/WireGuard/wireguard-android/blob/e7b3a3c118836e112620b1302a8ba1873ad4daac/ui/src/main/java/com/wireguard/android/QuickTileService.kt)
     */
    private fun startServiceFromTile() {
        if (DataStore.serviceMode.getBlocking() == Key.MODE_VPN && BaseVpnService.prepare(this) != null) {
            // Consent is missing: the foreground service start is doomed anyway, and
            // VpnService would then try to launch the consent activity from the background,
            // which Android 10 forbids. Go straight through the activity.
            startViaRequestActivity()
            return
        }

        val started = try {
            resolveRepository().startService()
            true
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException on Android 12+.
            Logs.w("Tile cannot start the foreground service directly", e)
            false
        } catch (e: SecurityException) {
            Logs.w("Tile cannot start the foreground service directly", e)
            false
        }
        if (!started) startViaRequestActivity()
    }

    /**
     * Collapses the panel onto [VpnRequestActivity], which asks for VPN consent when needed and
     * then starts the service from a foreground activity context.
     */
    private fun startViaRequestActivity() {
        val intent = Intent(this, VpnRequestActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTile() {
        scope.launch {
            val state = DataStore.serviceState
            val profileName = if (state.connected) {
                withContext(Dispatchers.IO) {
                    val profileId = DataStore.currentProfile.get()
                    if (profileId <= 0L) {
                        null
                    } else {
                        ThroneDatabase.proxyDao.getById(profileId)?.displayNameForService()
                    }
                }
            } else {
                null
            }
            updateTile(state, profileName)
        }
    }
}
