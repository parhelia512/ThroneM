package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.HostTextField
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.code
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.disable_post_quantum
import io.throneproj.thronem.resources.ech
import io.throneproj.thronem.resources.ech_config
import io.throneproj.thronem.resources.ech_query_server_name
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.enable
import io.throneproj.thronem.resources.experimental_settings
import io.throneproj.thronem.resources.extra_headers
import io.throneproj.thronem.resources.grain
import io.throneproj.thronem.resources.https
import io.throneproj.thronem.resources.naive_idle_timeout
import io.throneproj.thronem.resources.naive_insecure_concurrency
import io.throneproj.thronem.resources.naive_insecure_concurrency_summary
import io.throneproj.thronem.resources.naive_tunnel_timeout
import io.throneproj.thronem.resources.nfc
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.password_opt
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.protocol
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.search
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.speed
import io.throneproj.thronem.resources.timelapse
import io.throneproj.thronem.resources.traffic
import io.throneproj.thronem.resources.tuic_congestion_controller
import io.throneproj.thronem.resources.udp_over_tcp
import io.throneproj.thronem.resources.username_opt
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun NaiveSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: NaiveSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        NaiveSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        naiveSettings(uiState as NaiveUiState, viewModel)
    }
}

private fun LazyListScope.naiveSettings(
    uiState: NaiveUiState,
    viewModel: NaiveSettingsViewModel,
) {
    val protos = listOf("https", "quic")

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
        TextFieldPreference(
            value = uiState.username,
            onValueChange = { viewModel.setUsername(it) },
            title = { Text(stringResource(Res.string.username_opt)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.person, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.username)) },
            valueToText = { it },
        )
        PasswordPreference(
            value = uiState.password,
            onValueChange = { viewModel.setPassword(it) },
            title = { Text(stringResource(Res.string.password_opt)) },
        )
        ListPreference(
            value = uiState.proto,
            values = protos,
            onValueChange = { viewModel.setProto(it) },
            title = { Text(stringResource(Res.string.protocol)) },
            icon = {
                MaskedIcon(Res.drawable.https, IconMaskColors.IconLightGreen)
            },
            summary = { Text(contentOrUnset(uiState.proto)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
        ListPreference(
            value = uiState.quicCongestionControl,
            values = congestionControlsWithEmpty,
            onValueChange = { viewModel.setQuicCongestionControl(it) },
            title = { Text(stringResource(Res.string.tuic_congestion_controller)) },
            enabled = uiState.proto == "quic",
            icon = {
                MaskedIcon(Res.drawable.traffic, IconMaskColors.IconLavender)
            },
            summary = { Text(contentOrUnset(uiState.quicCongestionControl)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
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
            value = uiState.extraHeaders,
            onValueChange = { viewModel.setExtraHeaders(it) },
            title = { Text(stringResource(Res.string.extra_headers)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.code, IconMaskColors.IconLightYellow)
            },
            summary = { Text(contentOrUnset(uiState.extraHeaders)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                HostTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.insecureConcurrency,
            onValueChange = { viewModel.setInsecureConcurrency(it) },
            title = { Text(stringResource(Res.string.naive_insecure_concurrency)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(Res.drawable.speed, IconMaskColors.IconCoral, IconMaskShapes.risk())
            },
            summary = {
                val text = if (uiState.insecureConcurrency == 0) {
                    stringResource(Res.string.not_set)
                } else {
                    uiState.insecureConcurrency.toString()
                }
                Text(text)
            },
            textField = { value, onValueChange, onOk ->
                Column {
                    Text(
                        text = stringResource(Res.string.naive_insecure_concurrency_summary),
                        modifier = Modifier.padding(16.dp),
                    )

                    UIntegerTextField(value, onValueChange, onOk)
                }
            },
        )
    }

    item("category_experimental") {
        PreferenceCategory(
            text = { Text(stringResource(Res.string.experimental_settings)) },
        )
    }
    preferenceGroup(key = "udp_over_tcp") {
        SwitchPreference(
            value = uiState.udpOverTcp,
            onValueChange = { viewModel.setUdpOverTcp(it) },
            title = { Text(stringResource(Res.string.udp_over_tcp)) },
            icon = { Spacer(Modifier.size(24.dp)) },
        )
    }

    item("category_ech") {
        PreferenceCategory(text = { Text(stringResource(Res.string.ech)) })
    }
    preferenceGroup(key = "ech") {
        SwitchPreference(
            value = uiState.enableEch,
            onValueChange = { viewModel.setEnableEch(it) },
            title = { Text(stringResource(Res.string.enable)) },
            icon = {
                MaskedIcon(Res.drawable.security, IconMaskColors.IconCoral, IconMaskShapes.risk())
            },
        )
        TextFieldPreference(
            value = uiState.echConfig,
            onValueChange = { viewModel.setEchConfig(it) },
            title = { Text(stringResource(Res.string.ech_config)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.nfc, IconMaskColors.IconCyan, IconMaskShapes.credential())
            },
            enabled = uiState.enableEch,
            summary = { Text(contentOrUnset(uiState.echConfig)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.echQueryServerName,
            onValueChange = { viewModel.setEchQueryServerName(it) },
            title = { Text(stringResource(Res.string.ech_query_server_name)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.search, IconMaskColors.IconLightPink)
            },
            enabled = uiState.enableEch,
            summary = { Text(contentOrUnset(uiState.echQueryServerName)) },
            valueToText = { it },
        )
    }
}
