package io.throneproj.thronem

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.StrictMode
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import io.throneproj.thronem.bg.AppChangeReceiver
import io.throneproj.thronem.bg.CoreBox
import io.throneproj.thronem.bg.DefaultNetworkMonitor
import io.throneproj.thronem.bg.RouteAssetUpdater
import io.throneproj.thronem.bg.SubscriptionUpdater
import io.throneproj.thronem.compose.clearClipboardImageCache
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.di.initThroneMKoin
import io.throneproj.thronem.ktx.invariantDirectoryPathString
import io.throneproj.thronem.ktx.runOnDefaultDispatcher
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.repository.AndroidRepository
import io.throneproj.thronem.repository.SagerRepository
import io.throneproj.thronem.utils.CrashHandler
import io.throneproj.thronem.utils.PackageCache
import io.throneproj.thronem.utils.copyBundledRuleSetAssetsIfNeeded
import go.Seq
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.runBlocking
import java.io.File
import androidx.work.Configuration as WorkConfiguration

class Application : Application(),
    WorkConfiguration.Provider {

    private lateinit var repository: AndroidRepository

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        repository = SagerRepository(this, isMainProcess)
    }

    val externalAssets: File by lazy { getExternalFilesDir(null) ?: filesDir }
    private val appId by lazy { packageName }
    private val process by lazy { tryGetProcessName() }
    val isMainProcess get() = process == appId

    override fun onCreate() {
        super.onCreate()
        initThroneMKoin(repository)

        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler)

        if (isMainProcess) runOnIoDispatcher {
            clearClipboardImageCache(cacheDir)
        }

        if (isMainProcess) runOnDefaultDispatcher {
            // The component state may drift from the preference, e.g. after a backup restore.
            val hidden = DataStore.hideLauncherIcon.get()
            if (LauncherIcon.hidden != hidden) LauncherIcon.hidden = hidden
        }

        runOnDefaultDispatcher {
            PackageCache.register(this@Application)
        }

        Seq.setContext(this)
        runOnDefaultDispatcher {
            repository.updateNotificationChannels()
        }

        // init core
        externalAssets.mkdirs()
        val rulesProvider = DataStore.rulesProvider.getBlocking()
        val isExpert = DataStore.isExpert.getBlocking()
        if (rulesProvider == RuleProvider.OFFICIAL) {
            runBlocking { copyBundledRuleSetAssetsIfNeeded() }
        }
        Libbox.setup(
            SetupOptions().apply {
                basePath = filesDir.invariantDirectoryPathString()
                workingPath = filesDir.invariantDirectoryPathString()
                tempPath = noBackupFilesDir.invariantDirectoryPathString()
                logMaxLines = DataStore.logMaxLine.getBlocking().toLong()
                appVersion = BuildConfig.VERSION_NAME
                appMarketingVersion = BuildConfig.VERSION_NAME
            },
        )
        CoreBox.start()

        if (isMainProcess) runOnDefaultDispatcher {
            runCatching {
                SubscriptionUpdater.reconfigureUpdater()
                RouteAssetUpdater.reconfigureUpdater()
            }
            registerReceiver(
                AppChangeReceiver(),
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addDataScheme("package")
                },
            )
        }

        runBlocking {
            DefaultNetworkMonitor.start()
        }

        if (isExpert) StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build(),
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runOnDefaultDispatcher {
            repository.updateNotificationChannels()
        }
    }

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setDefaultProcessName(appId)
            .build()

    @SuppressLint("PrivateApi")
    private fun tryGetProcessName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()

        // Using the same technique as Application.getProcessName() for older devices
        // Using reflection since ActivityThread is an internal API
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val methodName = "currentProcessName"
            val getProcessName = activityThread.getDeclaredMethod(methodName)
            return getProcessName.invoke(null) as String
        } catch (_: Exception) {
            return appId
        }
    }

}
