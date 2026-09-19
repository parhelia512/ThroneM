package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.DurationTextField
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
import io.throneproj.thronem.resources.add_road
import io.throneproj.thronem.resources.allow_insecure
import io.throneproj.thronem.resources.alpn
import io.throneproj.thronem.resources.block
import io.throneproj.thronem.resources.cert_public_key_sha256
import io.throneproj.thronem.resources.certificates
import io.throneproj.thronem.resources.client_certificate
import io.throneproj.thronem.resources.client_key
import io.throneproj.thronem.resources.compare_arrows
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.ech
import io.throneproj.thronem.resources.ech_config
import io.throneproj.thronem.resources.ech_query_server_name
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.enable
import io.throneproj.thronem.resources.flight_takeoff
import io.throneproj.thronem.resources.lock
import io.throneproj.thronem.resources.lock_open
import io.throneproj.thronem.resources.multiple_stop
import io.throneproj.thronem.resources.mutual_tls
import io.throneproj.thronem.resources.nfc
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.quic
import io.throneproj.thronem.resources.quic_connection_receive_window
import io.throneproj.thronem.resources.quic_disable_path_mtu_discovery
import io.throneproj.thronem.resources.quic_idle_timeout
import io.throneproj.thronem.resources.quic_initial_packet_size
import io.throneproj.thronem.resources.quic_keep_alive_period
import io.throneproj.thronem.resources.quic_max_concurrent_streams
import io.throneproj.thronem.resources.quic_stream_receive_window
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.search
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.texture
import io.throneproj.thronem.resources.timelapse
import io.throneproj.thronem.resources.toc
import io.throneproj.thronem.resources.transform
import io.throneproj.thronem.resources.tuic_congestion_controller
import io.throneproj.thronem.resources.tuic_disable_sni
import io.throneproj.thronem.resources.tuic_reduce_rtt
import io.throneproj.thronem.resources.tuic_udp_relay_mode
import io.throneproj.thronem.resources.uuid
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.wb_sunny
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun TuicSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: TuicSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        TuicSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        tuicSettings(uiState as TuicUiState, viewModel)
    }
}

private fun LazyListScope.tuicSettings(
    uiState: TuicUiState,
    viewModel: TuicSettingsViewModel,
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
        TextFieldPreference(
            value = uiState.uuid,
            onValueChange = { viewModel.setUuid(it) },
            title = { Text(stringResource(Res.string.uuid)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.person, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.uuid)) },
            valueToText = { it },
        )
        PasswordPreference(
            value = uiState.token,
            onValueChange = { viewModel.setToken(it) },
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
                    resource = Res.drawable.vpn_key,
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
        ListPreference(
            value = uiState.udpRelayMode,
            values = listOf("native", "quic", "UDP over Stream"),
            onValueChange = { viewModel.setUdpRelayMode(it) },
            title = { Text(stringResource(Res.string.tuic_udp_relay_mode)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.add_road,
                    color = IconMaskColors.IconLightGreen,
                    shape = IconMaskShapes.route(),
                )
            },
            summary = { Text(contentOrUnset(uiState.udpRelayMode)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
        ListPreference(
            value = uiState.congestionController,
            values = congestionControls,
            onValueChange = { viewModel.setCongestionController(it) },
            title = { Text(stringResource(Res.string.tuic_congestion_controller)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.compare_arrows,
                    color = IconMaskColors.IconLightGreen,
                )
            },
            summary = { Text(contentOrUnset(uiState.congestionController)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
        SwitchPreference(
            value = uiState.disableSNI,
            onValueChange = { viewModel.setDisableSNI(it) },
            title = { Text(stringResource(Res.string.tuic_disable_sni)) },
            icon = {
                MaskedIcon(Res.drawable.block, color = IconMaskColors.IconWarmGray)
            },
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
        SwitchPreference(
            value = uiState.zeroRTT,
            onValueChange = { viewModel.setZeroRTT(it) },
            title = { Text(stringResource(Res.string.tuic_reduce_rtt)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.flight_takeoff,
                    color = IconMaskColors.IconCoral,
                )
            },
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
    }

    item("category_quic") {
        PreferenceCategory(text = { Text(stringResource(Res.string.quic)) })
    }
    preferenceGroup(key = "stream_receive_window") {
        TextFieldPreference(
            value = uiState.streamReceiveWindow,
            onValueChange = { viewModel.setStreamReceiveWindow(it) },
            title = { Text(stringResource(Res.string.quic_stream_receive_window)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(Res.drawable.texture, IconMaskColors.IconWarmGray)
            },
            summary = {
                val text = if (uiState.streamReceiveWindow == 0) {
                    stringResource(Res.string.not_set)
                } else {
                    uiState.streamReceiveWindow.toString()
                }
                Text(text)
            },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.connectionReceiveWindow,
            onValueChange = { viewModel.setConnectionReceiveWindow(it) },
            title = { Text(stringResource(Res.string.quic_connection_receive_window)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(Res.drawable.transform, IconMaskColors.IconWarmGray)
            },
            summary = {
                val text = if (uiState.connectionReceiveWindow == 0) {
                    stringResource(Res.string.not_set)
                } else {
                    uiState.connectionReceiveWindow.toString()
                }
                Text(text)
            },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        SwitchPreference(
            value = uiState.disablePathMtuDiscovery,
            onValueChange = { viewModel.setDisablePathMtuDiscovery(it) },
            title = { Text(stringResource(Res.string.quic_disable_path_mtu_discovery)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.multiple_stop,
                    color = IconMaskColors.IconLightYellow,
                    shape = IconMaskShapes.route(),
                )
            },
        )
        TextFieldPreference(
            value = uiState.idleTimeout,
            onValueChange = { viewModel.setIdleTimeout(it) },
            title = { Text(stringResource(Res.string.quic_idle_timeout)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.timelapse, IconMaskColors.IconWarmGray)
            },
            summary = { Text(contentOrUnset(uiState.idleTimeout)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                DurationTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.keepAlivePeriod,
            onValueChange = { viewModel.setKeepAlivePeriod(it) },
            title = { Text(stringResource(Res.string.quic_keep_alive_period)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.timelapse, IconMaskColors.IconWarmGray)
            },
            summary = { Text(contentOrUnset(uiState.keepAlivePeriod)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                DurationTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.maxConcurrentStreams,
            onValueChange = { viewModel.setMaxConcurrentStreams(it) },
            title = { Text(stringResource(Res.string.quic_max_concurrent_streams)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(Res.drawable.transform, IconMaskColors.IconWarmGray)
            },
            summary = {
                val text = if (uiState.maxConcurrentStreams == 0) {
                    stringResource(Res.string.not_set)
                } else {
                    uiState.maxConcurrentStreams.toString()
                }
                Text(text)
            },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.initialPacketSize,
            onValueChange = { viewModel.setInitialPacketSize(it) },
            title = { Text(stringResource(Res.string.quic_initial_packet_size)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(Res.drawable.texture, IconMaskColors.IconWarmGray)
            },
            summary = {
                val text = if (uiState.initialPacketSize == 0) {
                    stringResource(Res.string.not_set)
                } else {
                    uiState.initialPacketSize.toString()
                }
                Text(text)
            },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
    }

    item("category_ech") {
        PreferenceCategory(text = { Text(stringResource(Res.string.ech)) })
    }
    preferenceGroup(key = "ech") {
        SwitchPreference(
            value = uiState.ech,
            onValueChange = { viewModel.setEch(it) },
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
                MaskedIcon(Res.drawable.nfc, IconMaskColors.IconCoral, IconMaskShapes.risk())
            },
            enabled = uiState.ech,
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
                MaskedIcon(
                    resource = Res.drawable.search,
                    color = IconMaskColors.IconLightYellow,
                    shape = IconMaskShapes.credential(),
                )
            },
            enabled = uiState.ech,
            summary = { Text(contentOrUnset(uiState.echQueryServerName)) },
            valueToText = { it },
        )
    }

    item("category_mtls") {
        PreferenceCategory(text = { Text(stringResource(Res.string.mutual_tls)) })
    }
    preferenceGroup(key = "mtls_cert") {
        TextFieldPreference(
            value = uiState.clientCert,
            onValueChange = { viewModel.setClientCert(it) },
            title = { Text(stringResource(Res.string.client_certificate)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.lock,
                    color = IconMaskColors.IconCyan,
                    shape = IconMaskShapes.credential(),
                )
            },
            summary = { Text(contentOrUnset(uiState.clientCert)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
        TextFieldPreference(
            value = uiState.clientKey,
            onValueChange = { viewModel.setClientKey(it) },
            title = { Text(stringResource(Res.string.client_key)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.vpn_key,
                    color = IconMaskColors.IconCyan,
                    shape = IconMaskShapes.credential(),
                )
            },
            summary = { Text(contentOrUnset(uiState.clientKey)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
    }
}
