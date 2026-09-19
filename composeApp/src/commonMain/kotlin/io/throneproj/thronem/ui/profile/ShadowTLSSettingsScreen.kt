package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.allow_insecure
import io.throneproj.thronem.resources.alpn
import io.throneproj.thronem.resources.cert_public_key_sha256
import io.throneproj.thronem.resources.certificates
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.fingerprint
import io.throneproj.thronem.resources.lock_open
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.protocol_version
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.security_settings
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.toc
import io.throneproj.thronem.resources.update
import io.throneproj.thronem.resources.utls_fingerprint
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.wb_sunny
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShadowTLSSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: ShadowTLSSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        ShadowTLSSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        shadowTlsSettings(uiState as ShadowTLSUiState, viewModel)
    }
}

private fun LazyListScope.shadowTlsSettings(
    uiState: ShadowTLSUiState,
    viewModel: ShadowTLSSettingsViewModel,
) {
    preferenceGroup(key = "name") {
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
    preferenceGroup(key = "address") {
        TextFieldPreference(
            value = uiState.address,
            onValueChange = { viewModel.setAddress(it) },
            title = { Text(stringResource(Res.string.server_address)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.router, color = IconMaskColors.IconCyan)
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
                    color = IconMaskColors.IconCyan,
                )
            },
            summary = { Text(contentOrUnset(uiState.port)) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        ListPreference(
            value = uiState.protocolVersion,
            values = listOf(2, 3),
            onValueChange = { viewModel.setProtocolVersion(it) },
            title = { Text(stringResource(Res.string.protocol_version)) },
            icon = {
                MaskedIcon(Res.drawable.update, color = IconMaskColors.IconCyan)
            },
            summary = { Text(uiState.protocolVersion.toString()) },
            type = ListPreferenceType.DROPDOWN_MENU,
        )
        PasswordPreference(
            value = uiState.password,
            onValueChange = { viewModel.setPassword(it) },
        )
    }

    item("category_tls") {
        PreferenceCategory(text = { Text(stringResource(Res.string.security_settings)) })
    }
    preferenceGroup(key = "server_name") {
        TextFieldPreference(
            value = uiState.sni,
            onValueChange = { viewModel.setSni(it) },
            title = { Text(stringResource(Res.string.sni)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.copyright, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.sni)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.alpn,
            onValueChange = { viewModel.setAlpn(it) },
            title = { Text(stringResource(Res.string.alpn)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.toc, color = IconMaskColors.IconLightBlue)
            },
            summary = { Text(contentOrUnset(uiState.alpn)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.certificates,
            onValueChange = { viewModel.setCertificates(it) },
            title = { Text(stringResource(Res.string.certificates)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.vpn_key,
                    color = IconMaskColors.IconLightOrange,
                    shape = IconMaskShapes.credential(),
                )
            },
            summary = { Text(contentOrUnset(uiState.certificates)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.certPublicKeySha256,
            onValueChange = { viewModel.setCertPublicKeySha256(it) },
            title = { Text(stringResource(Res.string.cert_public_key_sha256)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.wb_sunny, IconMaskColors.IconLightYellow)
            },
            summary = { Text(contentOrUnset(uiState.certPublicKeySha256)) },
            valueToText = { it },
        )
        SwitchPreference(
            value = uiState.allowInsecure,
            onValueChange = { viewModel.setAllowInsecure(it) },
            title = { Text(stringResource(Res.string.allow_insecure)) },
            icon = {
                MaskedIcon(
                    Res.drawable.lock_open,
                    color = IconMaskColors.IconCoral,
                    shape = IconMaskShapes.risk(),
                )
            },
        )
        ListPreference(
            value = uiState.utlsFingerprint,
            values = fingerprints,
            onValueChange = { viewModel.setUtlsFingerprint(it) },
            title = { Text(stringResource(Res.string.utls_fingerprint)) },
            icon = {
                MaskedIcon(
                    Res.drawable.fingerprint,
                    color = IconMaskColors.IconCyan,
                )
            },
            summary = { Text(contentOrUnset(uiState.utlsFingerprint)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
    }
}
