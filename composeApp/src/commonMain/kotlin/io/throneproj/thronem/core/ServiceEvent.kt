package io.throneproj.thronem.core

import io.throneproj.thronem.bg.ServiceAlert
import io.throneproj.thronem.bg.ServiceState
import io.throneproj.thronem.bg.SpeedStats

sealed interface ServiceEvent {
    data class State(val state: ServiceState, val profileName: String?) : ServiceEvent
    data class Speed(val stats: SpeedStats) : ServiceEvent
    data class Alert(val alert: ServiceAlert) : ServiceEvent
}
