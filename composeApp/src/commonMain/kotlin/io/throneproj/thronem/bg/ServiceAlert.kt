package io.throneproj.thronem.bg

sealed interface ServiceAlert {
    data class Common(val message: String) : ServiceAlert
    data object NeedWifiPermission : ServiceAlert
}
