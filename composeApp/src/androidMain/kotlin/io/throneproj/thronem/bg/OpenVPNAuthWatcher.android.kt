package io.throneproj.thronem.bg

import android.content.Context
import io.throneproj.thronem.ktx.toList
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.openvpn_authentication
import io.throneproj.thronem.vpn.firstVpnAuthPending
import kotlinx.coroutines.flow.map

object OpenVPNAuthWatcher {

    private val watcher = VpnAuthNotificationWatcher(
        notificationId = 4,
        channelId = "service-openvpn-auth",
        title = Res.string.openvpn_authentication,
        logLabel = "openvpn auth watcher",
        pending = {
            subscribeOpenVPNStatus().map { update ->
                firstVpnAuthPending(
                    endpoints = update.endpoints().toList(),
                    state = { it.state },
                    challengeId = { status ->
                        status.challenge?.id
                    },
                    tag = { it.endpointTag },
                )
            }
        },
    )

    fun start(context: Context) = watcher.start(context)

    fun stop(context: Context) = watcher.stop(context)
}
