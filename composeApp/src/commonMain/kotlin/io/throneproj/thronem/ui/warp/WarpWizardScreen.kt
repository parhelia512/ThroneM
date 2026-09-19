package io.throneproj.thronem.ui.warp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.DropDownSelector
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.material3.Button
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Switch
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.expand_more
import io.throneproj.thronem.resources.shuffle
import io.throneproj.thronem.resources.warp_advanced
import io.throneproj.thronem.resources.warp_browser
import io.throneproj.thronem.resources.warp_endpoint
import io.throneproj.thronem.resources.warp_endpoint_desc
import io.throneproj.thronem.resources.warp_force_new
import io.throneproj.thronem.resources.warp_force_new_desc
import io.throneproj.thronem.resources.warp_include_reserved
import io.throneproj.thronem.resources.warp_include_reserved_desc
import io.throneproj.thronem.resources.warp_junk_packets
import io.throneproj.thronem.resources.warp_keepalive
import io.throneproj.thronem.resources.warp_keepalive_desc
import io.throneproj.thronem.resources.warp_license
import io.throneproj.thronem.resources.warp_license_desc
import io.throneproj.thronem.resources.warp_masque_desc
import io.throneproj.thronem.resources.warp_masque_endpoint_ip
import io.throneproj.thronem.resources.warp_masque_endpoint_ip_desc
import io.throneproj.thronem.resources.warp_masque_idle
import io.throneproj.thronem.resources.warp_masque_keepalive_sec
import io.throneproj.thronem.resources.warp_masque_network
import io.throneproj.thronem.resources.warp_masque_port
import io.throneproj.thronem.resources.warp_masque_sni
import io.throneproj.thronem.resources.warp_masque_sni_desc
import io.throneproj.thronem.resources.warp_masque_tuning_desc
import io.throneproj.thronem.resources.warp_masquerade_domain
import io.throneproj.thronem.resources.warp_masquerade_hint
import io.throneproj.thronem.resources.warp_masquerade_protocol
import io.throneproj.thronem.resources.warp_obfuscate
import io.throneproj.thronem.resources.warp_obfuscate_desc
import io.throneproj.thronem.resources.warp_register
import io.throneproj.thronem.resources.warp_registering
import io.throneproj.thronem.resources.warp_transport
import io.throneproj.thronem.resources.warp_wizard_desc
import io.throneproj.thronem.resources.warp_wizard_title
import io.throneproj.thronem.warp.MasqueAccount
import io.throneproj.thronem.warp.WarpEndpoints
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private val MASQUERADE_PROTOCOLS = listOf("quic", "dns", "stun", "sip")
private val MASQUERADE_BROWSERS = listOf("chrome", "firefox", "curl")

@Composable
fun WarpWizardScreen(
    onBackPress: () -> Unit,
) {
    val viewModel: WarpWizardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Column(modifier = Modifier.fillMaxSize()) {
        CapsuleTopBar(
            navigationIcon = {
                SimpleIconButton(
                    imageVector = vectorResource(Res.drawable.arrow_back),
                    contentDescription = stringResource(Res.string.back),
                    onClick = onBackPress,
                )
            },
            title = { Text(stringResource(Res.string.warp_wizard_title)) },
            scrollBehavior = scrollBehavior,
        )
        WarpWizardContent(uiState, viewModel, onBackPress)
    }
}

@Composable
private fun WarpWizardContent(
    uiState: WarpWizardUiState,
    viewModel: WarpWizardViewModel,
    onBackPress: () -> Unit,
) {
    var advancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Cloudflare WARP",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.warp_wizard_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // §130 — WARP transport: WireGuard (default) or MASQUE (CONNECT-IP over
        // HTTP/3/2 — a different exit pool, often foreign IPs).
        DropDownSelector(
            label = { Text(stringResource(Res.string.warp_transport)) },
            value = uiState.transport,
            values = listOf(WARP_TRANSPORT_WIREGUARD, WARP_TRANSPORT_MASQUE),
            onValueChange = viewModel::setTransport,
            displayValue = {
                if (it == WARP_TRANSPORT_MASQUE) "MASQUE" else "WireGuard"
            },
        )

        if (uiState.transport == WARP_TRANSPORT_MASQUE) {
            Text(
                text = stringResource(Res.string.warp_masque_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.transport == WARP_TRANSPORT_WIREGUARD) {
            // §025 — the only significant option lives outside Advanced, like the
            // LxBox wizard: obfuscation on/off, everything else is hidden.
            SwitchRow(
                title = stringResource(Res.string.warp_obfuscate),
                subtitle = stringResource(Res.string.warp_obfuscate_desc),
                checked = uiState.obfuscate,
                onCheckedChange = viewModel::setObfuscate,
            )
            if (uiState.obfuscate) {
                Text(
                    text = stringResource(Res.string.warp_masquerade_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MasqueOptions(uiState, viewModel)
        }

        AdvancedHeader(
            expanded = advancedExpanded,
            onToggle = { advancedExpanded = !advancedExpanded },
        )
        AnimatedVisibility(visible = advancedExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.transport == WARP_TRANSPORT_WIREGUARD) {
                    WireGuardAdvanced(uiState, viewModel)
                }

                SwitchRow(
                    title = stringResource(Res.string.warp_force_new),
                    subtitle = stringResource(Res.string.warp_force_new_desc),
                    checked = uiState.forceNew,
                    onCheckedChange = viewModel::setForceNew,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { viewModel.register(onSuccess = onBackPress) },
            enabled = !uiState.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.busy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.warp_registering))
            } else {
                Text(stringResource(Res.string.warp_register))
            }
        }

        if (uiState.message.isNotEmpty()) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = when (uiState.success) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    else -> Color.Unspecified
                },
            )
        }
    }
}

@Composable
private fun AdvancedHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.warp_advanced),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = vectorResource(Res.drawable.expand_more),
            contentDescription = stringResource(Res.string.warp_advanced),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** §130 — MASQUE transport options: HTTP version, endpoint, SNI, tuning. */
@Composable
private fun MasqueOptions(
    uiState: WarpWizardUiState,
    viewModel: WarpWizardViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DropDownSelector(
            label = { Text(stringResource(Res.string.warp_masque_network)) },
            value = uiState.masqueNetwork,
            values = listOf("auto", "h3", "h2"),
            onValueChange = viewModel::setMasqueNetwork,
            displayValue = { network ->
                when (network) {
                    "h3" -> "HTTP/3 (QUIC)"
                    "h2" -> "HTTP/2 (TCP)"
                    else -> "Auto (h3 → h2)"
                }
            },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.masqueEndpointIp,
                onValueChange = viewModel::setMasqueEndpointIp,
                label = { Text(stringResource(Res.string.warp_masque_endpoint_ip)) },
                placeholder = { Text(MasqueAccount.DEFAULT_SERVER) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            SimpleIconButton(
                imageVector = vectorResource(Res.drawable.shuffle),
                contentDescription = stringResource(Res.string.warp_masque_endpoint_ip),
                onClick = viewModel::rerollMasqueEndpoint,
            )
            OutlinedTextField(
                value = uiState.masquePort,
                onValueChange = viewModel::setMasquePort,
                label = { Text(stringResource(Res.string.warp_masque_port)) },
                placeholder = { Text(MasqueAccount.DEFAULT_PORT.toString()) },
                modifier = Modifier.width(96.dp),
                singleLine = true,
            )
        }
        Text(
            text = stringResource(Res.string.warp_masque_endpoint_ip_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.masqueSni,
                onValueChange = viewModel::setMasqueSni,
                label = { Text(stringResource(Res.string.warp_masque_sni)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            SimpleIconButton(
                imageVector = vectorResource(Res.drawable.shuffle),
                contentDescription = stringResource(Res.string.warp_masque_sni),
                onClick = viewModel::rerollMasqueSni,
            )
        }
        Text(
            text = stringResource(Res.string.warp_masque_sni_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.masqueIdleMinutes.takeIf { it > 0 }?.toString().orEmpty(),
                onValueChange = { viewModel.setMasqueIdleMinutes(it.toIntOrNull() ?: 0) },
                label = { Text(stringResource(Res.string.warp_masque_idle)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.masqueKeepAliveSeconds.takeIf { it > 0 }?.toString().orEmpty(),
                onValueChange = { viewModel.setMasqueKeepAliveSeconds(it.toIntOrNull() ?: 0) },
                label = { Text(stringResource(Res.string.warp_masque_keepalive_sec)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Text(
            text = stringResource(Res.string.warp_masque_tuning_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** §025/§126 — WireGuard-only Advanced content (license, endpoint, AWG). */
@Composable
private fun WireGuardAdvanced(
    uiState: WarpWizardUiState,
    viewModel: WarpWizardViewModel,
) {
    OutlinedTextField(
        value = uiState.license,
        onValueChange = viewModel::setLicense,
        label = { Text(stringResource(Res.string.warp_license)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Text(
        text = stringResource(Res.string.warp_license_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = uiState.endpoint,
            onValueChange = viewModel::setEndpoint,
            label = { Text(stringResource(Res.string.warp_endpoint)) },
            placeholder = { Text(WarpEndpoints.DEFAULT_ENDPOINT) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        if (uiState.obfuscate) {
            Spacer(Modifier.width(8.dp))
            SimpleIconButton(
                imageVector = vectorResource(Res.drawable.shuffle),
                contentDescription = stringResource(Res.string.warp_endpoint),
                onClick = viewModel::rerollEndpoint,
            )
        }
    }
    Text(
        text = stringResource(Res.string.warp_endpoint_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = uiState.keepalive.takeIf { it > 0 }?.toString().orEmpty(),
        onValueChange = { viewModel.setKeepalive(it.toIntOrNull() ?: 0) },
        label = { Text(stringResource(Res.string.warp_keepalive)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Text(
        text = stringResource(Res.string.warp_keepalive_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SwitchRow(
        title = stringResource(Res.string.warp_include_reserved),
        subtitle = stringResource(Res.string.warp_include_reserved_desc),
        checked = uiState.includeReserved,
        onCheckedChange = viewModel::setIncludeReserved,
    )

    if (uiState.obfuscate) {
        DropDownSelector(
            label = { Text(stringResource(Res.string.warp_masquerade_protocol)) },
            value = uiState.masqueradeProtocol,
            values = MASQUERADE_PROTOCOLS,
            onValueChange = viewModel::setMasqueradeProtocol,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.masqueradeDomain,
                onValueChange = viewModel::setMasqueradeDomain,
                label = { Text(stringResource(Res.string.warp_masquerade_domain)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            SimpleIconButton(
                imageVector = vectorResource(Res.drawable.shuffle),
                contentDescription = stringResource(Res.string.warp_masquerade_domain),
                onClick = viewModel::rerollMasqueradeDomain,
            )
        }
        if (uiState.masqueradeProtocol == "quic") {
            DropDownSelector(
                label = { Text(stringResource(Res.string.warp_browser)) },
                value = uiState.masqueradeBrowser,
                values = MASQUERADE_BROWSERS,
                onValueChange = viewModel::setMasqueradeBrowser,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.junkCount.toString(),
                onValueChange = { viewModel.setJunkCount(it.toIntOrNull() ?: 0) },
                label = { Text("Jc") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.junkMin.toString(),
                onValueChange = { viewModel.setJunkMin(it.toIntOrNull() ?: 0) },
                label = { Text("Jmin") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.junkMax.toString(),
                onValueChange = { viewModel.setJunkMax(it.toIntOrNull() ?: 0) },
                label = { Text("Jmax") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
