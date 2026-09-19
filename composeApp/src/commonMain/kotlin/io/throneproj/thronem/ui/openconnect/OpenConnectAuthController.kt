package io.throneproj.thronem.ui.openconnect

import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OpenConnectBrowserResult
import io.nekohasekai.libbox.OpenConnectFormValues
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.openconnect.OpenConnectBean
import io.throneproj.thronem.fmt.openconnect.OpenConnectFormEntry
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.toList
import io.throneproj.thronem.vpn.OPENCONNECT_FIELD_PASSWORD
import io.throneproj.thronem.vpn.OPENCONNECT_STATE_AUTH_PENDING
import io.throneproj.thronem.vpn.OpenConnectAuthChallengeState
import io.throneproj.thronem.vpn.OpenConnectBrowserResultState
import io.throneproj.thronem.vpn.OpenConnectEndpointState
import io.throneproj.thronem.vpn.PendingVpnAuth
import io.throneproj.thronem.vpn.VpnAuthSession
import io.throneproj.thronem.vpn.toState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext

typealias PendingOpenConnectAuth = PendingVpnAuth<OpenConnectAuthChallengeState>

/**
 * Long-lived owner of the OpenConnect endpoint status subscription.
 *
 * The auth challenge lives in the core as part of the endpoint state; this
 * controller only mirrors it. Dismissing the dialog hides it locally
 * (the challenge stays pending in the core and remains reachable from the
 * status page) while [cancelAuthChallenge] actually aborts authentication.
 */
class OpenConnectAuthController(
    private val coreClient: CoreClient = GlobalContext.get().get(),
) {
    private val session = VpnAuthSession(
        subscribe = {
            coreClient.subscribeOpenConnectStatus().map { update ->
                update.endpoints().toList().map { it.toState() }
            }
        },
        pendingOf = { endpoint ->
            endpoint.authChallenge?.takeIf {
                endpoint.state == OPENCONNECT_STATE_AUTH_PENDING
            }?.let { PendingVpnAuth(endpoint.tag, it) }
        },
        challengeId = { it.id },
        logLabel = "openconnect",
    )

    val endpoints: StateFlow<List<OpenConnectEndpointState>>
        get() = session.endpoints

    val pendingDialogAuth: StateFlow<PendingOpenConnectAuth?>
        get() = session.pendingDialogAuth

    /** Hide the dialog for this challenge without cancelling authentication. */
    fun dismissDialog(endpointTag: String, challengeId: String) {
        session.dismissDialog(endpointTag, challengeId)
    }

    /** @return an error message, or null on success. */
    suspend fun submitAuthChallenge(
        endpointTag: String,
        challenge: OpenConnectAuthChallengeState,
        formValues: Map<String, String>?,
        browserResult: OpenConnectBrowserResultState?,
    ): String? {
        val error = session.perform("submit openconnect auth challenge") {
            val response = when {
                formValues != null -> {
                    val values = OpenConnectFormValues()
                    for ((name, value) in formValues) {
                        values.add(name, value)
                    }
                    Libbox.newOpenConnectAuthFormResponse(values)
                }

                browserResult != null -> {
                    val result = OpenConnectBrowserResult(browserResult.finalUrl)
                    for ((name, value) in browserResult.cookies) {
                        if (value.isNotEmpty()) {
                            result.addCookie(name, value)
                        }
                    }
                    for ((name, value) in browserResult.headers) {
                        value.lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { result.addHeader(name, it) }
                    }
                    Libbox.newOpenConnectBrowserAuthResponse(result)
                }

                else -> error("no openconnect auth response to submit")
            }
            coreClient.submitOpenConnectAuthResponse(endpointTag, challenge.id, response)
            if (challenge.form != null && formValues != null) {
                persistFormEntries(endpointTag, challenge, formValues)
            }
        }
        return error
    }

    /**
     * Remember the submitted non-secret answers in the profile's
     * [OpenConnectBean.formEntries] so the next connection can replay
     * them without interaction.
     */
    private suspend fun persistFormEntries(
        endpointTag: String,
        challenge: OpenConnectAuthChallengeState,
        values: Map<String, String>,
    ) {
        val newEntries = challenge.form?.fields.orEmpty().mapNotNull { field ->
            if (field.kind == OPENCONNECT_FIELD_PASSWORD) return@mapNotNull null
            val value = values[field.submissionKey] ?: return@mapNotNull null
            if (value.isBlank()) return@mapNotNull null
            OpenConnectFormEntry(
                formId = challenge.id,
                submissionKey = field.submissionKey,
                name = field.name,
                value = value,
            )
        }
        if (newEntries.isEmpty()) return
        try {
            val profile = findProfile(endpointTag) ?: return
            val bean = profile.requireBean() as OpenConnectBean
            val replacedKeys = newEntries.mapTo(HashSet()) { it.formId to it.submissionKey }
            bean.formEntries = (
                bean.formEntries.filterNot { (it.formId to it.submissionKey) in replacedKeys } +
                    newEntries
                ).toList()
            profile.putBean(bean)
            ProfileManager.updateProfile(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logs.w("persist openconnect form entries", e)
        }
    }

    private suspend fun findProfile(endpointTag: String): ProxyEntity? {
        ThroneDatabase.proxyDao.getById(DataStore.currentProfile.get())?.let { current ->
            if (current.requireBean() is OpenConnectBean && current.displayName() == endpointTag) {
                return current
            }
        }
        val candidates = ThroneDatabase.proxyDao.getAll().filter { entity ->
            entity.requireBean() is OpenConnectBean && entity.displayName() == endpointTag
        }
        return candidates.singleOrNull()
    }

    /** @return an error message, or null on success. */
    suspend fun cancelAuthChallenge(endpointTag: String, challengeId: String): String? =
        session.perform("cancel openconnect auth challenge") {
            coreClient.cancelOpenConnectAuthChallenge(endpointTag, challengeId)
        }
}
