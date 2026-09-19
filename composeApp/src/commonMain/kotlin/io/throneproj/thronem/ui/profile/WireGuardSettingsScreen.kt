package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultilineTextField
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
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.encrypted
import io.throneproj.thronem.resources.fingerprint
import io.throneproj.thronem.resources.listen_port
import io.throneproj.thronem.resources.mtu
import io.throneproj.thronem.resources.persistent_keepalive_interval
import io.throneproj.thronem.resources.pre_shared_key
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.public_icon
import io.throneproj.thronem.resources.replay
import io.throneproj.thronem.resources.reserved
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.ssh_private_key
import io.throneproj.thronem.resources.stream
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.warp_masquerade_domain
import io.throneproj.thronem.resources.warp_masquerade_protocol
import io.throneproj.thronem.resources.warp_obfuscate
import io.throneproj.thronem.resources.wireguard_local_address
import io.throneproj.thronem.resources.wireguard_public_key
import io.throneproj.thronem.ui.NavRoutes
import org.jetbrains.compose.resources.stringResource

@Composable
fun WireGuardSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: WireGuardSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        WireGuardSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        wireGuardSettings(uiState as WireGuardUiState, viewModel)
    }
}

private fun LazyListScope.wireGuardSettings(
    uiState: WireGuardUiState,
    viewModel: WireGuardSettingsViewModel,
) {
    preferenceGroup {
        TextFieldPreference(
            value = uiState.name,
            onValueChange = { viewModel.setName(it) },
            title = { Text(stringResource(Res.string.profile_name)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.emoji_symbols,
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
            textToValue = { it.toIntOrNull() ?: 51820 },
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
        TextFieldPreference(
            value = uiState.localAddress,
            onValueChange = { viewModel.setLocalAddress(it) },
            title = { Text(stringResource(Res.string.wireguard_local_address)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domain,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.localAddress)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        PasswordPreference(
            value = uiState.privateKey,
            onValueChange = { viewModel.setPrivateKey(it) },
            title = { Text(stringResource(Res.string.ssh_private_key)) },
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
            title = { Text(stringResource(Res.string.wireguard_public_key)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.copyright, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.publicKey)) },
            valueToText = { it },
        )
        PasswordPreference(
            value = uiState.preSharedKey,
            onValueChange = { viewModel.setPreSharedKey(it) },
            title = { Text(stringResource(Res.string.pre_shared_key)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.vpn_key,
                    color = IconMaskColors.IconCoral,
                    shape = IconMaskShapes.risk(),
                )
            },
        )
        TextFieldPreference(
            value = uiState.mtu,
            onValueChange = { viewModel.setMtu(it) },
            title = { Text(stringResource(Res.string.mtu)) },
            textToValue = { it.toIntOrNull() ?: 1420 },
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
            value = uiState.reserved,
            onValueChange = { viewModel.setReserved(it) },
            title = { Text(stringResource(Res.string.reserved)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.fingerprint,
                    color = IconMaskColors.IconCoral,
                    shape = IconMaskShapes.route(),
                )
            },
            summary = { Text(contentOrUnset(uiState.reserved)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.listenPort,
            onValueChange = { viewModel.setListenPort(it) },
            title = { Text(stringResource(Res.string.listen_port)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.stream,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.listenPort)) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.persistentKeepaliveInterval,
            onValueChange = { viewModel.setPersistentKeepaliveInterval(it) },
            title = { Text(stringResource(Res.string.persistent_keepalive_interval)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.replay,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = {
                Text(contentOrUnset(uiState.persistentKeepaliveInterval))
            },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
    }

    item("category_awg") {
        PreferenceCategory(text = { Text(stringResource(Res.string.warp_obfuscate)) })
    }
    preferenceGroup {
        TextFieldPreference(
            value = uiState.awgJc,
            onValueChange = { viewModel.setAwgJc(it) },
            title = { Text("Jc") },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(if (uiState.awgJc > 0) uiState.awgJc.toString() else "")) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.awgJmin,
            onValueChange = { viewModel.setAwgJmin(it) },
            title = { Text("Jmin") },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(if (uiState.awgJmin > 0) uiState.awgJmin.toString() else "")) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.awgJmax,
            onValueChange = { viewModel.setAwgJmax(it) },
            title = { Text("Jmax") },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(if (uiState.awgJmax > 0) uiState.awgJmax.toString() else "")) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.awgS1,
            onValueChange = { viewModel.setAwgS1(it) },
            title = { Text("S1") },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(if (uiState.awgS1 > 0) uiState.awgS1.toString() else "")) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.awgS2,
            onValueChange = { viewModel.setAwgS2(it) },
            title = { Text("S2") },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(if (uiState.awgS2 > 0) uiState.awgS2.toString() else "")) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.awgH1,
            onValueChange = { viewModel.setAwgH1(it) },
            title = { Text("H1") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgH1)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgH2,
            onValueChange = { viewModel.setAwgH2(it) },
            title = { Text("H2") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgH2)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgH3,
            onValueChange = { viewModel.setAwgH3(it) },
            title = { Text("H3") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgH3)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgH4,
            onValueChange = { viewModel.setAwgH4(it) },
            title = { Text("H4") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgH4)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgIp,
            onValueChange = { viewModel.setAwgIp(it) },
            title = { Text(stringResource(Res.string.warp_masquerade_protocol)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domino_mask,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgIp)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgId,
            onValueChange = { viewModel.setAwgId(it) },
            title = { Text(stringResource(Res.string.warp_masquerade_domain)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domino_mask,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgId)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.awgIb,
            onValueChange = { viewModel.setAwgIb(it) },
            title = { Text("Ib") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.domino_mask,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.awgIb)) },
            valueToText = { it },
        )
    }
}
