package io.throneproj.thronem.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PortTextField
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.PreferenceGroupDefaults
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.ktx.intListN
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.allow_insecure
import io.throneproj.thronem.resources.allow_insecure_sum
import io.throneproj.thronem.resources.alpn
import io.throneproj.thronem.resources.assistant_direction
import io.throneproj.thronem.resources.block
import io.throneproj.thronem.resources.bolt
import io.throneproj.thronem.resources.border_inner
import io.throneproj.thronem.resources.cert_public_key_sha256
import io.throneproj.thronem.resources.certificates
import io.throneproj.thronem.resources.client_certificate
import io.throneproj.thronem.resources.client_key
import io.throneproj.thronem.resources.code
import io.throneproj.thronem.resources.compare_arrows
import io.throneproj.thronem.resources.computer_cancel
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.domino_mask
import io.throneproj.thronem.resources.early_data_header_name
import io.throneproj.thronem.resources.ech
import io.throneproj.thronem.resources.ech_config
import io.throneproj.thronem.resources.ech_query_server_name
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.enable
import io.throneproj.thronem.resources.enable_brutal
import io.throneproj.thronem.resources.enhanced_encryption
import io.throneproj.thronem.resources.grpc_service_name
import io.throneproj.thronem.resources.http_headers
import io.throneproj.thronem.resources.http_host
import io.throneproj.thronem.resources.http_path
import io.throneproj.thronem.resources.http_upgrade_host
import io.throneproj.thronem.resources.http_upgrade_path
import io.throneproj.thronem.resources.language
import io.throneproj.thronem.resources.layers
import io.throneproj.thronem.resources.lock
import io.throneproj.thronem.resources.multiple_stop
import io.throneproj.thronem.resources.mutual_tls
import io.throneproj.thronem.resources.mux_number
import io.throneproj.thronem.resources.mux_preference
import io.throneproj.thronem.resources.mux_strategy
import io.throneproj.thronem.resources.mux_type
import io.throneproj.thronem.resources.nfc
import io.throneproj.thronem.resources.numbers
import io.throneproj.thronem.resources.padding
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.reality_public_key
import io.throneproj.thronem.resources.reality_short_id
import io.throneproj.thronem.resources.route
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.search
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.security_settings
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.speed
import io.throneproj.thronem.resources.stream
import io.throneproj.thronem.resources.texture
import io.throneproj.thronem.resources.timer
import io.throneproj.thronem.resources.tls_camouflage_settings
import io.throneproj.thronem.resources.tls_fragment
import io.throneproj.thronem.resources.tls_fragment_fallback_delay
import io.throneproj.thronem.resources.tls_record_fragment
import io.throneproj.thronem.resources.tls_spoof
import io.throneproj.thronem.resources.tls_spoof_method
import io.throneproj.thronem.resources.toc
import io.throneproj.thronem.resources.tuic_disable_sni
import io.throneproj.thronem.resources.type_specimen
import io.throneproj.thronem.resources.utls_fingerprint
import io.throneproj.thronem.resources.v2ray_transport
import io.throneproj.thronem.resources.view_in_ar
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.wb_sunny
import io.throneproj.thronem.resources.ws_host
import io.throneproj.thronem.resources.ws_max_early_data
import io.throneproj.thronem.resources.ws_path
import io.throneproj.thronem.resources.xhttp_host
import io.throneproj.thronem.resources.xhttp_mode
import io.throneproj.thronem.resources.xhttp_path
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

private const val KEY_SECURITY = "security"

internal fun LazyListScope.headSettings(
    state: StandardV2RayUiState,
    viewModel: StandardV2RaySettingsViewModel<*>,
) {
    item("category_basic") {
        PreferenceCategory(text = { Text(stringResource(Res.string.proxy_cat)) })
    }
    preferenceGroup {
        TextFieldPreference(
            value = state.name,
            onValueChange = { viewModel.setName(it) },
            title = { Text(stringResource(Res.string.profile_name)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.emoji_symbols,
                    color = IconMaskColors.IconCyan,
                )
            },
            summary = { Text(contentOrUnset(state.name)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = state.address,
            onValueChange = { viewModel.setAddress(it) },
            title = { Text(stringResource(Res.string.server_address)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.router,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(state.address)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = state.port,
            onValueChange = { viewModel.setPort(it) },
            title = { Text(stringResource(Res.string.server_port)) },
            textToValue = { it.toIntOrNull() ?: 443 },
            icon = {
                MaskedIcon(
                    Res.drawable.directions_boat,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(state.port)) },
            textField = { value, onValueChange, onOk -> PortTextField(value, onValueChange, onOk) },
        )
    }
}

internal fun LazyListScope.tlsSettings(
    state: StandardV2RayUiState,
    viewModel: StandardV2RaySettingsViewModel<*>,
    scrollTo: (key: String) -> Unit,
) {
    val isTls = state.security == "tls"
    val isReality = state.realityPublicKey.isNotBlank()

    item("category_security") {
        PreferenceCategory(text = { Text(stringResource(Res.string.security_settings)) })
    }
    preferenceGroup(key = KEY_SECURITY) {
        ListPreference(
            value = state.security,
            values = listOf("", "tls"),
            onValueChange = {
                viewModel.setSecurity(it)
                if (it == "tls") {
                    scrollTo(KEY_SECURITY)
                }
            },
            title = { Text(stringResource(Res.string.security)) },
            icon = {
                MaskedIcon(
                    Res.drawable.layers,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(state.security)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
    }

    if (isTls) {
        preferenceGroup {
            TextFieldPreference(
                value = state.sni,
                onValueChange = { viewModel.setSni(it) },
                title = { Text(stringResource(Res.string.sni)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.copyright,
                        color = IconMaskColors.IconCyan,
                    )
                },
                summary = { Text(contentOrUnset(state.sni)) },
                valueToText = { it },
            )
            TextFieldPreference(
                value = state.alpn,
                onValueChange = { viewModel.setAlpn(it) },
                title = { Text(stringResource(Res.string.alpn)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.toc,
                        color = IconMaskColors.IconLightBlue,
                    )
                },
                summary = { Text(contentOrUnset(state.alpn)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = state.certificate,
                onValueChange = { viewModel.setCertificate(it) },
                title = { Text(stringResource(Res.string.certificates)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.vpn_key,
                        color = IconMaskColors.IconLightOrange,
                        shape = IconMaskShapes.credential(),
                    )
                },
                summary = { Text(contentOrUnset(state.certificate)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = state.certPublicKeySha256,
                onValueChange = { viewModel.setCertPublicKeySha256(it) },
                title = { Text(stringResource(Res.string.cert_public_key_sha256)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.wb_sunny,
                        color = IconMaskColors.IconLightYellow,
                    )
                },
                summary = { Text(contentOrUnset(state.certPublicKeySha256)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
            SwitchPreference(
                value = state.allowInsecure,
                onValueChange = { viewModel.setAllowInsecure(it) },
                title = { Text(stringResource(Res.string.allow_insecure)) },
                summary = { Text(stringResource(Res.string.allow_insecure_sum)) },
                icon = {
                    MaskedIcon(
                        Res.drawable.enhanced_encryption,
                        color = IconMaskColors.IconCoral,
                        shape = IconMaskShapes.risk(),
                    )
                },
            )
            if (!isReality) {
                SwitchPreference(
                    value = state.disableSNI,
                    onValueChange = { viewModel.setDisableSNI(it) },
                    title = { Text(stringResource(Res.string.tuic_disable_sni)) },
                    icon = {
                        MaskedIcon(
                            Res.drawable.block,
                            color = IconMaskColors.IconWarmGray,
                        )
                    },
                )
            }
            SwitchPreference(
                value = state.tlsFragment,
                onValueChange = { viewModel.setTlsFragment(it) },
                title = { Text(stringResource(Res.string.tls_fragment)) },
                enabled = !state.tlsRecordFragment,
                icon = {
                    MaskedIcon(
                        Res.drawable.texture,
                        color = IconMaskColors.IconLightBlue,
                    )
                },
            )
            TextFieldPreference(
                value = state.tlsFragmentFallbackDelay,
                onValueChange = { viewModel.setTlsFragmentFallbackDelay(it) },
                title = { Text(stringResource(Res.string.tls_fragment_fallback_delay)) },
                textToValue = { it },
                enabled = state.tlsFragment,
                icon = {
                    MaskedIcon(
                        Res.drawable.timer,
                        color = IconMaskColors.IconLightOrange,
                    )
                },
                summary = { Text(contentOrUnset(state.tlsFragmentFallbackDelay)) },
                valueToText = { it },
            )
            SwitchPreference(
                value = state.tlsRecordFragment,
                onValueChange = { viewModel.setTlsRecordFragment(it) },
                title = { Text(stringResource(Res.string.tls_record_fragment)) },
                enabled = !state.tlsFragment,
                icon = {
                    MaskedIcon(
                        Res.drawable.wb_sunny,
                        color = IconMaskColors.IconLavender,
                    )
                },
            )
        }

        item("category_tls_camouflage") {
            PreferenceCategory(text = { Text(stringResource(Res.string.tls_camouflage_settings)) })
        }
        preferenceGroup {
            ListPreference(
                value = state.utlsFingerprint,
                values = fingerprints,
                onValueChange = { viewModel.setUtlsFingerprint(it) },
                title = { Text(stringResource(Res.string.utls_fingerprint)) },
                icon = {
                    MaskedIcon(
                        Res.drawable.security,
                        color = IconMaskColors.IconCyan,
                    )
                },
                summary = { Text(contentOrUnset(state.utlsFingerprint)) },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = { AnnotatedString(it) },
            )
            TextFieldPreference(
                value = state.realityPublicKey,
                onValueChange = { viewModel.setRealityPublicKey(it) },
                title = { Text(stringResource(Res.string.reality_public_key)) },
                textToValue = { it },
                enabled = state.utlsFingerprint.isNotBlank(),
                icon = {
                    MaskedIcon(
                        Res.drawable.vpn_key,
                        color = IconMaskColors.IconLightBlue,
                    )
                },
                summary = { Text(contentOrUnset(state.realityPublicKey)) },
                valueToText = { it },
            )
            TextFieldPreference(
                value = state.realityShortID,
                onValueChange = { viewModel.setRealityShortID(it) },
                title = { Text(stringResource(Res.string.reality_short_id)) },
                textToValue = { it },
                enabled = isReality,
                icon = {
                    MaskedIcon(
                        Res.drawable.texture,
                        color = IconMaskColors.IconLightOrange,
                    )
                },
                summary = { Text(contentOrUnset(state.realityShortID)) },
                valueToText = { it },
            )
            if (!PlatformInfo.isAndroid) {
                TextFieldPreference(
                    value = state.tlsSpoof,
                    onValueChange = { viewModel.setTlsSpoof(it) },
                    title = { Text(stringResource(Res.string.tls_spoof)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.domino_mask,
                            color = IconMaskColors.IconWarmGray,
                        )
                    },
                    summary = { Text(contentOrUnset(state.tlsSpoof)) },
                    valueToText = { it },
                )
                ListPreference(
                    value = state.tlsSpoofMethod,
                    values = tlsSpoofMethod,
                    onValueChange = { viewModel.setTlsSpoofMethod(it) },
                    title = { Text(stringResource(Res.string.tls_spoof_method)) },
                    enabled = state.tlsSpoof.isNotBlank(),
                    icon = {
                        MaskedIcon(
                            Res.drawable.computer_cancel,
                            color = IconMaskColors.IconLightYellow,
                        )
                    },
                    summary = { Text(contentOrUnset(state.tlsSpoofMethod)) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(it) },
                )
            }
        }

        item("category_ech") { PreferenceCategory(text = { Text(stringResource(Res.string.ech)) }) }
        preferenceGroup {
            SwitchPreference(
                value = state.ech,
                onValueChange = { viewModel.setEch(it) },
                title = { Text(stringResource(Res.string.enable)) },
                icon = {
                    MaskedIcon(
                        resource = Res.drawable.security,
                        color = IconMaskColors.IconCoral,
                        shape = IconMaskShapes.risk(),
                    )
                },
            )
            TextFieldPreference(
                value = state.echConfig,
                onValueChange = { viewModel.setEchConfig(it) },
                title = { Text(stringResource(Res.string.ech_config)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        resource = Res.drawable.nfc,
                        color = IconMaskColors.IconLightBlue,
                        shape = IconMaskShapes.credential(),
                    )
                },
                enabled = state.ech,
                summary = { Text(contentOrUnset(state.echConfig)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = state.echQueryServerName,
                onValueChange = { viewModel.setEchQueryServerName(it) },
                title = { Text(stringResource(Res.string.ech_query_server_name)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.search,
                        color = IconMaskColors.IconLightOrange,
                    )
                },
                enabled = state.ech,
                summary = { Text(contentOrUnset(state.echQueryServerName)) },
                valueToText = { it },
            )
        }

        item("category_mutual_tls") {
            PreferenceCategory(text = { Text(stringResource(Res.string.mutual_tls)) })
        }
        preferenceGroup {
            TextFieldPreference(
                value = state.clientCert,
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
                summary = { Text(contentOrUnset(state.clientCert)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = state.clientKey,
                onValueChange = { viewModel.setClientKey(it) },
                title = { Text(stringResource(Res.string.client_key)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.vpn_key,
                        color = IconMaskColors.IconLavender,
                        shape = IconMaskShapes.credential(),
                    )
                },
                summary = { Text(contentOrUnset(state.clientKey)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
        }
    }
}

internal fun LazyListScope.muxSettings(
    state: StandardV2RayUiState,
    viewModel: StandardV2RaySettingsViewModel<*>,
) {
    item("category_mux") {
        PreferenceCategory(text = { Text(stringResource(Res.string.mux_preference)) })
    }
    preferenceGroup {
        SwitchPreference(
            value = state.enableMux,
            onValueChange = { viewModel.setEnableMux(it) },
            title = { Text(stringResource(Res.string.enable)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.multiple_stop,
                    color = IconMaskColors.IconLightPink,
                )
            },
        )
        AnimatedVisibility(visible = state.enableMux) {
            Column(verticalArrangement = PreferenceGroupDefaults.itemArrangement) {
                SwitchPreference(
                    value = state.brutal,
                    onValueChange = { viewModel.setBrutal(it) },
                    title = { Text(stringResource(Res.string.enable_brutal)) },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.bolt,
                            color = IconMaskColors.IconCoral,
                            shape = IconMaskShapes.risk(),
                        )
                    },
                )
                ListPreference(
                    value = state.muxType,
                    values = intListN(muxTypes.size),
                    onValueChange = { viewModel.setMuxType(it) },
                    title = { Text(stringResource(Res.string.mux_type)) },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.type_specimen,
                            color = IconMaskColors.IconLightGreen,
                        )
                    },
                    summary = { Text(muxTypes[state.muxType]) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(muxTypes[it]) },
                )
                ListPreference(
                    value = state.muxStrategy,
                    values = intListN(muxStrategies.size),
                    onValueChange = { viewModel.setMuxStrategy(it) },
                    title = { Text(stringResource(Res.string.mux_strategy)) },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.view_in_ar,
                            color = IconMaskColors.IconWarmGray,
                        )
                    },
                    summary = { Text(stringResource(muxStrategies[state.muxStrategy])) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(stringResource(muxStrategies[it])) },
                    enabled = !state.brutal,
                )
                TextFieldPreference(
                    value = state.muxNumber,
                    onValueChange = { viewModel.setMuxNumber(it) },
                    title = { Text(stringResource(Res.string.mux_number)) },
                    textToValue = { it.toIntOrNull() ?: 0 },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.numbers,
                            color = IconMaskColors.IconLightYellow,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(state.muxNumber.toString()) },
                    valueToText = { it.toString() },
                    enabled = !state.brutal,
                )
                SwitchPreference(
                    value = state.muxPadding,
                    onValueChange = { viewModel.setMuxPadding(it) },
                    title = { Text(stringResource(Res.string.padding)) },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.border_inner,
                            color = IconMaskColors.IconLightBlue,
                        )
                    },
                )
            }
        }
    }
}

internal fun LazyListScope.transportSettings(
    state: StandardV2RayUiState,
    viewModel: StandardV2RaySettingsViewModel<*>,
) {
    item("category_transport") {
        PreferenceCategory(text = { Text(stringResource(Res.string.v2ray_transport)) })
    }
    preferenceGroup {
        ListPreference(
            value = state.v2rayTransport,
            values =
                listOf(
                    "",
                    SingBoxOptions.TRANSPORT_WS,
                    SingBoxOptions.TRANSPORT_HTTP,
                    SingBoxOptions.TRANSPORT_GRPC,
                    SingBoxOptions.TRANSPORT_HTTPUPGRADE,
                    SingBoxOptions.TRANSPORT_XHTTP,
                    SingBoxOptions.TRANSPORT_QUIC,
                ),
            onValueChange = { viewModel.setTransport(it) },
            title = { Text(stringResource(Res.string.v2ray_transport)) },
            icon = {
                MaskedIcon(
                    Res.drawable.route,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(state.v2rayTransport)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(contentOrUnset(it)) },
        )
        when (state.v2rayTransport) {
            "",
            "tcp",
                -> Unit

            SingBoxOptions.TRANSPORT_HTTP -> {
                TextFieldPreference(
                    value = state.host,
                    onValueChange = { viewModel.setHost(it) },
                    title = { Text(stringResource(Res.string.http_host)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.language,
                            color = IconMaskColors.IconCyan,
                        )
                    },
                    summary = { Text(contentOrUnset(state.host)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                TextFieldPreference(
                    value = state.path,
                    onValueChange = { viewModel.setPath(it) },
                    title = { Text(stringResource(Res.string.http_path)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.assistant_direction,
                            color = IconMaskColors.IconLightOrange,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(contentOrUnset(state.path)) },
                    valueToText = { it },
                )
                TextFieldPreference(
                    value = state.headers,
                    onValueChange = { viewModel.setHeaders(it) },
                    title = { Text(stringResource(Res.string.http_headers)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.code,
                            color = IconMaskColors.IconLavender,
                        )
                    },
                    summary = { Text(contentOrUnset(state.headers)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
            }

            SingBoxOptions.TRANSPORT_WS -> {
                TextFieldPreference(
                    value = state.host,
                    onValueChange = { viewModel.setHost(it) },
                    title = { Text(stringResource(Res.string.ws_host)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.language,
                            color = IconMaskColors.IconCyan,
                        )
                    },
                    summary = { Text(contentOrUnset(state.host)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                TextFieldPreference(
                    value = state.path,
                    onValueChange = { viewModel.setPath(it) },
                    title = { Text(stringResource(Res.string.ws_path)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.assistant_direction,
                            color = IconMaskColors.IconLightOrange,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(contentOrUnset(state.path)) },
                    valueToText = { it },
                )
                TextFieldPreference(
                    value = state.headers,
                    onValueChange = { viewModel.setHeaders(it) },
                    title = { Text(stringResource(Res.string.http_headers)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.code,
                            color = IconMaskColors.IconLavender,
                        )
                    },
                    summary = { Text(contentOrUnset(state.headers)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                TextFieldPreference(
                    value = state.wsMaxEarlyData,
                    onValueChange = { viewModel.setWsMaxEarlyData(it) },
                    title = { Text(stringResource(Res.string.ws_max_early_data)) },
                    textToValue = { it.toIntOrNull() ?: 0 },
                    icon = {
                        MaskedIcon(
                            Res.drawable.compare_arrows,
                            color = IconMaskColors.IconLightGreen,
                        )
                    },
                    summary = { Text(contentOrUnset(state.wsMaxEarlyData)) },
                )
                TextFieldPreference(
                    value = state.wsEarlyDataHeaderName,
                    onValueChange = { viewModel.setWsEarlyDataHeaderName(it) },
                    title = { Text(stringResource(Res.string.early_data_header_name)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.stream,
                            color = IconMaskColors.IconWarmGray,
                        )
                    },
                    summary = { Text(contentOrUnset(state.wsEarlyDataHeaderName)) },
                    valueToText = { it },
                )
            }

            SingBoxOptions.TRANSPORT_GRPC -> {
                TextFieldPreference(
                    value = state.path,
                    onValueChange = { viewModel.setPath(it) },
                    title = { Text(stringResource(Res.string.grpc_service_name)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.assistant_direction,
                            color = IconMaskColors.IconLightGreen,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(contentOrUnset(state.path)) },
                    valueToText = { it },
                )
            }

            SingBoxOptions.TRANSPORT_HTTPUPGRADE -> {
                TextFieldPreference(
                    value = state.host,
                    onValueChange = { viewModel.setHost(it) },
                    title = { Text(stringResource(Res.string.http_upgrade_host)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.language,
                            color = IconMaskColors.IconCyan,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(contentOrUnset(state.host)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                TextFieldPreference(
                    value = state.path,
                    onValueChange = { viewModel.setPath(it) },
                    title = { Text(stringResource(Res.string.http_upgrade_path)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.assistant_direction,
                            color = IconMaskColors.IconLightOrange,
                        )
                    },
                    summary = { Text(contentOrUnset(state.path)) },
                    valueToText = { it },
                )
                TextFieldPreference(
                    value = state.headers,
                    onValueChange = { viewModel.setHeaders(it) },
                    title = { Text(stringResource(Res.string.http_headers)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.code,
                            color = IconMaskColors.IconLavender,
                        )
                    },
                    summary = { Text(contentOrUnset(state.headers)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
            }

            SingBoxOptions.TRANSPORT_XHTTP -> {
                // Host / path behave exactly like httpupgrade, with their own labels.
                TextFieldPreference(
                    value = state.host,
                    onValueChange = { viewModel.setHost(it) },
                    title = { Text(stringResource(Res.string.xhttp_host)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            resource = Res.drawable.language,
                            color = IconMaskColors.IconCyan,
                            shape = IconMaskShapes.route(),
                        )
                    },
                    summary = { Text(contentOrUnset(state.host)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                TextFieldPreference(
                    value = state.path,
                    onValueChange = { viewModel.setPath(it) },
                    title = { Text(stringResource(Res.string.xhttp_path)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.assistant_direction,
                            color = IconMaskColors.IconLightOrange,
                        )
                    },
                    summary = { Text(contentOrUnset(state.path)) },
                    valueToText = { it },
                )
                TextFieldPreference(
                    value = state.headers,
                    onValueChange = { viewModel.setHeaders(it) },
                    title = { Text(stringResource(Res.string.http_headers)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.code,
                            color = IconMaskColors.IconLavender,
                        )
                    },
                    summary = { Text(contentOrUnset(state.headers)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )

                // The remaining parameters are the core's V2RayXHTTPOptions
                // (docs-lx/lx-protocols-transports.md §1). Unset values are
                // omitted from the generated config, so the core defaults apply.
                val xhttp = state.xhttp
                ListPreference(
                    value = xhttp.mode,
                    // The core default is auto, so there is no "unset" entry.
                    values = listOf("auto", "packet-up", "stream-up", "stream-one"),
                    onValueChange = { viewModel.setXhttp(xhttp.copy(mode = it)) },
                    title = { Text(stringResource(Res.string.xhttp_mode)) },
                    icon = {
                        MaskedIcon(
                            Res.drawable.speed,
                            color = IconMaskColors.IconLightOrange,
                        )
                    },
                    summary = { Text(xhttp.mode) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(it) },
                )
                XhttpTextField("x_padding_bytes", xhttp.paddingBytes) {
                    viewModel.setXhttp(xhttp.copy(paddingBytes = it))
                }
                XhttpSwitch("no_grpc_header", xhttp.noGrpcHeader) {
                    viewModel.setXhttp(xhttp.copy(noGrpcHeader = it))
                }
                XhttpTextField("session_placement", xhttp.sessionPlacement) {
                    viewModel.setXhttp(xhttp.copy(sessionPlacement = it))
                }
                XhttpTextField("session_key", xhttp.sessionKey) {
                    viewModel.setXhttp(xhttp.copy(sessionKey = it))
                }
                XhttpTextField("seq_placement", xhttp.seqPlacement) {
                    viewModel.setXhttp(xhttp.copy(seqPlacement = it))
                }
                XhttpTextField("seq_key", xhttp.seqKey) {
                    viewModel.setXhttp(xhttp.copy(seqKey = it))
                }
                XhttpTextField("session_table", xhttp.sessionTable) {
                    viewModel.setXhttp(xhttp.copy(sessionTable = it))
                }
                XhttpTextField("session_length", xhttp.sessionLength) {
                    viewModel.setXhttp(xhttp.copy(sessionLength = it))
                }
                XhttpTextField("uplink_data_placement", xhttp.uplinkDataPlacement) {
                    viewModel.setXhttp(xhttp.copy(uplinkDataPlacement = it))
                }
                XhttpTextField("uplink_data_key", xhttp.uplinkDataKey) {
                    viewModel.setXhttp(xhttp.copy(uplinkDataKey = it))
                }
                XhttpTextField("uplink_chunk_size", xhttp.uplinkChunkSize) {
                    viewModel.setXhttp(xhttp.copy(uplinkChunkSize = it))
                }
                XhttpTextField("uplink_http_method", xhttp.uplinkHttpMethod) {
                    viewModel.setXhttp(xhttp.copy(uplinkHttpMethod = it))
                }
                XhttpSwitch("x_padding_obfs_mode", xhttp.paddingObfsMode) {
                    viewModel.setXhttp(xhttp.copy(paddingObfsMode = it))
                }
                XhttpTextField("x_padding_key", xhttp.paddingKey) {
                    viewModel.setXhttp(xhttp.copy(paddingKey = it))
                }
                XhttpTextField("x_padding_header", xhttp.paddingHeader) {
                    viewModel.setXhttp(xhttp.copy(paddingHeader = it))
                }
                XhttpTextField("x_padding_placement", xhttp.paddingPlacement) {
                    viewModel.setXhttp(xhttp.copy(paddingPlacement = it))
                }
                XhttpTextField("x_padding_method", xhttp.paddingMethod) {
                    viewModel.setXhttp(xhttp.copy(paddingMethod = it))
                }
                XhttpTextField("sc_max_each_post_bytes", xhttp.scMaxEachPostBytes) {
                    viewModel.setXhttp(xhttp.copy(scMaxEachPostBytes = it))
                }
                XhttpTextField("sc_min_posts_interval_ms", xhttp.scMinPostsIntervalMs) {
                    viewModel.setXhttp(xhttp.copy(scMinPostsIntervalMs = it))
                }
                XhttpTextField("sc_stream_up_server_secs", xhttp.scStreamUpServerSecs) {
                    viewModel.setXhttp(xhttp.copy(scStreamUpServerSecs = it))
                }
                XhttpTextField(
                    "sc_max_buffered_posts",
                    xhttp.scMaxBufferedPosts.takeIf { it >= 0 }?.toString().orEmpty(),
                ) {
                    viewModel.setXhttp(xhttp.copy(scMaxBufferedPosts = it.toIntOrNull() ?: -1))
                }
                XhttpSwitch("no_sse_header", xhttp.noSseHeader) {
                    viewModel.setXhttp(xhttp.copy(noSseHeader = it))
                }
                XhttpTextField("xmux.max_concurrency", xhttp.maxConcurrency) {
                    viewModel.setXhttp(xhttp.copy(maxConcurrency = it))
                }
                XhttpTextField("xmux.max_connections", xhttp.maxConnections) {
                    viewModel.setXhttp(xhttp.copy(maxConnections = it))
                }
                XhttpTextField("xmux.c_max_reuse_times", xhttp.cMaxReuseTimes) {
                    viewModel.setXhttp(xhttp.copy(cMaxReuseTimes = it))
                }
                XhttpTextField("xmux.h_max_request_times", xhttp.hMaxRequestTimes) {
                    viewModel.setXhttp(xhttp.copy(hMaxRequestTimes = it))
                }
                XhttpTextField("xmux.h_max_reusable_secs", xhttp.hMaxReusableSecs) {
                    viewModel.setXhttp(xhttp.copy(hMaxReusableSecs = it))
                }
                XhttpTextField(
                    "xmux.h_keep_alive_period",
                    xhttp.hKeepAlivePeriod.takeIf { it >= 0 }?.toString().orEmpty(),
                ) {
                    viewModel.setXhttp(xhttp.copy(hKeepAlivePeriod = it.toIntOrNull() ?: -1))
                }
            }

            SingBoxOptions.TRANSPORT_QUIC -> Unit
        }
    }
}

/**
 * Compact row for an XHTTP parameter whose title is its wire name — these are
 * protocol field names (like `x_padding_bytes`), not user-facing copy.
 */
@Composable
private fun XhttpTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextFieldPreference(
        value = value,
        onValueChange = onValueChange,
        title = { Text(label) },
        textToValue = { it },
        icon = {
            MaskedIcon(
                Res.drawable.code,
                color = IconMaskColors.IconLavender,
            )
        },
        summary = { Text(contentOrUnset(value)) },
        valueToText = { it },
    )
}

@Composable
private fun XhttpSwitch(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        value = value,
        onValueChange = onValueChange,
        title = { Text(label) },
        icon = {
            MaskedIcon(
                Res.drawable.bolt,
                color = IconMaskColors.IconLightYellow,
            )
        },
    )
}
