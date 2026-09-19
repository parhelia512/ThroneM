package io.throneproj.thronem.bg.proto

import io.throneproj.thronem.bg.BaseService
import io.throneproj.thronem.bg.RunningConfigRegistry
import io.throneproj.thronem.bg.ServiceEventPublisher
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.fmt.ConfigBuildResult
import io.throneproj.thronem.ktx.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

class ProxyInstance(profile: ProxyEntity, var service: BaseService.Interface? = null) :
    BoxInstance(profile) {

    var displayProfileName = profile.displayNameForService()

    var trafficLooper: TrafficLooper? = null

    /** Owns the traffic looper, cancelled in [close] so nothing outlives this instance. */
    private val looperScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun buildConfig(): ConfigBuildResult = super.buildConfig().also {
        Logs.d(it.configJson)
        if (DataStore.isExpert.get()) {
            Logs.d("trafficProfiles: " + it.metadata.trafficProfiles.toString())
        }
    }

    override fun launch() {
        RunningConfigRegistry.metadata = metadata
        super.launch() // start box
        looperScope.launch {
            val data = service?.data ?: return@launch
            trafficLooper = TrafficLooper(
                coreClient = GlobalContext.get().get<CoreClient>(),
                metadata = metadata,
                scope = looperScope,
                onSpeedUpdate = { stats ->
                    ServiceEventPublisher.publishSpeed(stats)
                    data.notification.apply {
                        if (canPostSpeed()) onSpeed(stats)
                    }
                },
            )
            trafficLooper?.start()
        }
    }

    override fun close() {
        super.close()
        RunningConfigRegistry.metadata = null
        runBlocking {
            trafficLooper?.stop()
            trafficLooper = null
        }
        looperScope.cancel()
    }
}
