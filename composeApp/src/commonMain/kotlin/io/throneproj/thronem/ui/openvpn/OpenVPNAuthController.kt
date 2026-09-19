package io.throneproj.thronem.ui.openvpn

import io.nekohasekai.libbox.OpenVPNChallengeResponse
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.vpn.OPENVPN_STATE_AUTH_PENDING
import io.throneproj.thronem.vpn.OpenVPNChallengeState
import io.throneproj.thronem.vpn.OpenVPNEndpointState
import io.throneproj.thronem.vpn.PendingVpnAuth
import io.throneproj.thronem.vpn.VpnAuthSession
import io.throneproj.thronem.vpn.toState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import io.throneproj.thronem.ktx.toList
import org.koin.core.context.GlobalContext

typealias PendingOpenVPNAuth = PendingVpnAuth<OpenVPNChallengeState>

class OpenVPNAuthController(
    private val coreClient: CoreClient = GlobalContext.get().get(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val session = VpnAuthSession(
        subscribe = {
            coreClient.subscribeOpenVPNStatus().map { update ->
                update.endpoints().toList().map { it.toState() }
            }
        },
        pendingOf = { endpoint ->
            endpoint.challenge?.takeIf {
                endpoint.state == OPENVPN_STATE_AUTH_PENDING
            }?.let { PendingVpnAuth(endpoint.tag, it) }
        },
        challengeId = { it.id },
        logLabel = "openvpn",
        dispatcher = ioDispatcher,
    )

    val endpoints: StateFlow<List<OpenVPNEndpointState>>
        get() = session.endpoints

    val pendingDialogAuth: StateFlow<PendingOpenVPNAuth?>
        get() = session.pendingDialogAuth

    fun dismissDialog(endpointTag: String, challengeId: String) {
        session.dismissDialog(endpointTag, challengeId)
    }

    /** @return an error message, or null on success. */
    suspend fun submitAuthChallenge(
        endpointTag: String,
        challenge: OpenVPNChallengeState,
        username: String,
        password: String,
        secret: String,
    ): String? = session.perform("submit openvpn auth challenge") {
        val response = OpenVPNChallengeResponse().apply {
            this.username = username
            this.password = password
            this.secret = secret
        }
        coreClient.submitOpenVPNChallengeResponse(endpointTag, challenge.id, response)
    }

    /** @return an error message, or null on success. */
    suspend fun cancelAuthChallenge(endpointTag: String, challengeId: String): String? =
        session.perform("cancel openvpn auth challenge") {
            coreClient.cancelOpenVPNChallenge(endpointTag, challengeId)
        }
}
