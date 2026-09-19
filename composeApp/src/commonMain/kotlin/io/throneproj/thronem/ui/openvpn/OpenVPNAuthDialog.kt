package io.throneproj.thronem.ui.openvpn

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.QRCodeDialog
import io.throneproj.thronem.compose.ScrollableDialog
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.auth_later
import io.throneproj.thronem.resources.auth_open_url
import io.throneproj.thronem.resources.share_qr_nfc
import io.throneproj.thronem.resources.auth_submit
import io.throneproj.thronem.resources.auth_verifying
import io.throneproj.thronem.resources.openvpn_authentication
import io.throneproj.thronem.ui.LocalSnackbarEmitter
import io.throneproj.thronem.ui.StringOrRes
import io.throneproj.thronem.vpn.OPENVPN_CHALLENGE_CREDENTIALS
import io.throneproj.thronem.vpn.OPENVPN_CHALLENGE_OPEN_URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import androidx.compose.material3.TextButton as Material3TextButton

/**
 * Global dialog for a pending OpenVPN auth challenge.
 *
 * Dismissing it (back press, outside touch or "Later") only hides the
 * dialog; the challenge stays pending in the core and can be picked up again
 * from the OpenVPN status page.
 */
@Composable
fun OpenVPNAuthDialog(
    pending: PendingOpenVPNAuth,
    controller: OpenVPNAuthController,
    showError: (String) -> Unit,
    onDismissed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val snackbar = LocalSnackbarEmitter.current
    val challenge = pending.challenge
    var username by remember(pending.endpointTag, challenge.id) {
        mutableStateOf(challenge.username)
    }
    var password by remember(pending.endpointTag, challenge.id) { mutableStateOf("") }
    var secret by remember(pending.endpointTag, challenge.id) { mutableStateOf("") }
    var submitting by remember(challenge.id) { mutableStateOf(false) }
    var submitted by remember(challenge.id) { mutableStateOf(false) }
    var showQr by remember(pending.endpointTag, challenge.id) { mutableStateOf(false) }
    var nowEpochSeconds by remember { mutableLongStateOf(Clock.System.now().epochSeconds) }
    val expired = challenge.deadline in 1..nowEpochSeconds
    val editable = !submitting && !expired

    if (challenge.deadline > 0) {
        LaunchedEffect(challenge.id) {
            while (true) {
                delay(1000)
                nowEpochSeconds = Clock.System.now().epochSeconds
            }
        }
    }

    fun dismiss() {
        controller.dismissDialog(pending.endpointTag, challenge.id)
        onDismissed()
    }

    fun submit() {
        scope.launch {
            submitting = true
            val error = controller.submitAuthChallenge(
                endpointTag = pending.endpointTag,
                challenge = challenge,
                username = if (challenge.kind == OPENVPN_CHALLENGE_CREDENTIALS) username else "",
                password = if (challenge.kind == OPENVPN_CHALLENGE_CREDENTIALS) password else "",
                secret = secret,
            )
            submitting = false
            if (error != null) {
                showError(error)
            } else {
                submitted = true
            }
        }
    }

    ScrollableDialog(
        onDismissRequest = ::dismiss,
        confirmButton = {
            if (!submitted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (challenge.kind == OPENVPN_CHALLENGE_OPEN_URL && challenge.url.isNotEmpty()) {
                        TextButton(stringResource(Res.string.auth_open_url)) {
                            runCatching { uriHandler.openUri(challenge.url) }
                                .onFailure { showError(it.readableMessage) }
                        }
                        TextButton(stringResource(Res.string.share_qr_nfc)) {
                            showQr = true
                        }
                    }
                    if (challenge.answerable) {
                        Material3TextButton(
                            enabled = editable,
                            onClick = ::submit,
                        ) {
                            Text(stringResource(Res.string.auth_submit))
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.auth_later), onClick = ::dismiss)
        },
        title = { Text(stringResource(Res.string.openvpn_authentication)) },
        text = {
            if (submitted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        stringResource(Res.string.auth_verifying),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            } else {
                OpenVPNAuthChallengeContent(
                    challenge = challenge,
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    secret = secret,
                    onSecretChange = { secret = it },
                    nowEpochSeconds = nowEpochSeconds,
                    enabled = editable,
                )
            }
        },
    )
    if (showQr && challenge.url.isNotEmpty()) {
        QRCodeDialog(
            url = challenge.url,
            name = pending.endpointTag,
            onDismiss = { showQr = false },
            showSnackbar = { snackbar.show(StringOrRes.Direct(it)) },
        )
    }
}
