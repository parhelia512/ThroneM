package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.domain
import io.throneproj.thronem.resources.domino_mask
import io.throneproj.thronem.resources.masque_idle_timeout
import io.throneproj.thronem.resources.masque_keep_alive
import io.throneproj.thronem.resources.masque_private_key
import io.throneproj.thronem.resources.masque_server_public_key
import io.throneproj.thronem.resources.masque_vhttp
import io.throneproj.thronem.resources.mtu
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.public_icon
import io.throneproj.thronem.resources.replay
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.ssh_private_key
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.wireguard_local_address
import io.throneproj.thronem.ui.NavRoutes
import org.jetbrains.compose.resources.stringResource

@Composable
fun MasqueSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: MasqueSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        MasqueSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        masqueSettings(uiState as MasqueUiState, viewModel)
    }
}

private fun LazyListScope.masqueSettings(
    uiState: MasqueUiState,
    viewModel: MasqueSettingsViewModel,
) {
    preferenceGroup {
        TextFieldPreference(
            value = uiState.name,
            onValueChange = { viewModel.setName(it) },
            title = { Text(stringResource(Res.string.profile_name)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domino_mask,
                    color = IconMaskColors.IconCyan,
                )
            },
            summary = { Text(contentOrUnset(uiState.name)) },
            valueToText = { it },
        )
    }

    item("category_proxy") {
        PreferenceCategory(text = { Text(stringResource(Res.string.proxy_cat)) })
    }
    preferenceGroup {
        TextFieldPreference(
            value = uiState.address,
            onValueChange = { viewModel.setAddress(it) },
            title = { Text(stringResource(Res.string.server_address)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.router,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.address)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.port,
            onValueChange = { viewModel.setPort(it) },
            title = { Text(stringResource(Res.string.server_port)) },
            textToValue = { it.toIntOrNull() ?: 443 },
            icon = {
                MaskedIcon(
                    Res.drawable.directions_boat,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(uiState.port)) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        PasswordPreference(
            value = uiState.privateKey,
            onValueChange = { viewModel.setPrivateKey(it) },
            title = { Text(stringResource(Res.string.masque_private_key)) },
            icon = {
                MaskedIcon(
                    Res.drawable.vpn_key,
                    color = IconMaskColors.IconLightGreen,
                    shape = IconMaskShapes.credential(),
                )
            },
        )
        TextFieldPreference(
            value = uiState.publicKey,
            onValueChange = { viewModel.setPublicKey(it) },
            title = { Text(stringResource(Res.string.masque_server_public_key)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.copyright,
                    color = IconMaskColors.IconCyan,
                    shape = IconMaskShapes.credential(),
                )
            },
            summary = { Text(contentOrUnset(uiState.publicKey)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.localIp,
            onValueChange = { viewModel.setLocalIp(it) },
            title = { Text(stringResource(Res.string.wireguard_local_address) + " (IPv4)") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domain,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.localIp)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.localIpv6,
            onValueChange = { viewModel.setLocalIpv6(it) },
            title = { Text(stringResource(Res.string.wireguard_local_address) + " (IPv6)") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domain,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.localIpv6)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.vhttp,
            onValueChange = { viewModel.setVhttp(it) },
            title = { Text(stringResource(Res.string.masque_vhttp)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domino_mask,
                    color = IconMaskColors.IconLightYellow,
                )
            },
            summary = { Text(contentOrUnset(uiState.vhttp)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.sni,
            onValueChange = { viewModel.setSni(it) },
            title = { Text(stringResource(Res.string.sni)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domain,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.sni)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.mtu,
            onValueChange = { viewModel.setMtu(it) },
            title = { Text(stringResource(Res.string.mtu)) },
            textToValue = { it.toIntOrNull() ?: 1280 },
            icon = {
                MaskedIcon(
                    Res.drawable.public_icon,
                    color = IconMaskColors.IconLightYellow,
                )
            },
            summary = { Text(contentOrUnset(uiState.mtu.toString())) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.idleTimeout,
            onValueChange = { viewModel.setIdleTimeout(it) },
            title = { Text(stringResource(Res.string.masque_idle_timeout)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.replay,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(uiState.idleTimeout)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.keepAlive,
            onValueChange = { viewModel.setKeepAlive(it) },
            title = { Text(stringResource(Res.string.masque_keep_alive)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.replay,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.keepAlive)) },
            valueToText = { it },
        )
    }
}
