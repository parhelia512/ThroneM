package io.throneproj.thronem.bg

import android.content.Context
import io.throneproj.thronem.ktx.toList
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.openconnect_authentication
import io.throneproj.thronem.vpn.firstVpnAuthPending
import kotlinx.coroutines.flow.map

object OpenConnectAuthWatcher {

    private val watcher = VpnAuthNotificationWatcher(
        notificationId = 3,
        channelId = "service-openconnect-auth",
        title = Res.string.openconnect_authentication,
        logLabel = "openconnect auth watcher",
        pending = {
            subscribeOpenConnectStatus().map { update ->
                firstVpnAuthPending(
                    endpoints = update.endpoints().toList(),
                    state = { it.state },
                    challengeId = { status ->
                        status.authChallenge?.id
                    },
                    tag = { it.endpointTag },
                )
            }
        },
    )

    fun start(context: Context) = watcher.start(context)

    fun stop(context: Context) = watcher.stop(context)
}
