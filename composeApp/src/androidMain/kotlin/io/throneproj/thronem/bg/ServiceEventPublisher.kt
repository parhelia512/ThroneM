package io.throneproj.thronem.bg

import io.throneproj.thronem.core.CoreEventBus
import io.throneproj.thronem.core.ServiceEvent
import io.throneproj.thronem.ktx.Logs

/**
 * Service-side publisher for [CoreEventBus]. In the single-process libbox
 * layout the UI collects these events directly — no wire serialization.
 */
object ServiceEventPublisher {

    fun publishState(state: ServiceState, profileName: String?) {
        CoreEventBus.events.tryEmit(ServiceEvent.State(state, profileName))
    }

    fun publishSpeed(stats: SpeedStats) {
        CoreEventBus.events.tryEmit(ServiceEvent.Speed(stats))
    }

    fun publishAlert(alert: ServiceAlert) {
        CoreEventBus.events.tryEmit(ServiceEvent.Alert(alert))
    }
}
