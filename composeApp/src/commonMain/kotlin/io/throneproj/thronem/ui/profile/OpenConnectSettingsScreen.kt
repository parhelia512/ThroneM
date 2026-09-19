package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.Preference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.android
import io.throneproj.thronem.resources.auto
import io.throneproj.thronem.resources.category
import io.throneproj.thronem.resources.certificate_authority
import io.throneproj.thronem.resources.clear_remembered_authentication_answers
import io.throneproj.thronem.resources.client_certificate
import io.throneproj.thronem.resources.client_key
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.dns
import io.throneproj.thronem.resources.domain
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.enhanced_encryption
import io.throneproj.thronem.resources.fingerprint
import io.throneproj.thronem.resources.insecure
import io.throneproj.thronem.resources.language
import io.throneproj.thronem.resources.local_hostname
import io.throneproj.thronem.resources.lock
import io.throneproj.thronem.resources.machine_certificate
import io.throneproj.thronem.resources.machine_key
import io.throneproj.thronem.resources.openconnect_allow_insecure_crypto
import io.throneproj.thronem.resources.openconnect_auth_group
import io.throneproj.thronem.resources.openconnect_authentication
import io.throneproj.thronem.resources.openconnect_flavor
import io.throneproj.thronem.resources.openconnect_reported_os
import io.throneproj.thronem.resources.password
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.security_settings
import io.throneproj.thronem.resources.tls_peer_fingerprint
import io.throneproj.thronem.resources.tls_server_name
import io.throneproj.thronem.resources.user_agent
import io.throneproj.thronem.resources.username
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.resources.vpn_server_url
import io.throneproj.thronem.resources.warning_amber
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpenConnectSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel = profileEditorViewModel(profileId, isSubscription) {
        OpenConnectSettingsViewModel()
    }
    ProfileSettingsScreenScaffold(
        Res.string.profile_config,
        viewModel,
        onResult,
        onOpenConfigEditor,
    ) { state, _ ->
        openConnectSettings(state as OpenConnectUiState, viewModel)
    }
}

private fun LazyListScope.openConnectSettings(
    state: OpenConnectUiState,
    viewModel: OpenConnectSettingsViewModel,
) {
    val flavors = listOf(
        "",
        "anyconnect",
        "gp",
        "fortinet",
        "f5",
        "pulse",
        "nc",
    )
    val pulseReportedOS = listOf(
        "",
        "linux",
        "linux-64",
        "win",
        "mac-intel",
        "android",
        "apple-ios",
    )

    preferenceGroup {
        TextFieldPreference(
            value = state.name,
            onValueChange = viewModel::setName,
            title = { Text(stringResource(Res.string.profile_name)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.name)) },
            icon = { MaskedIcon(Res.drawable.emoji_symbols, IconMaskColors.IconCyan) },
        )
    }
    if (state.formEntries.isNotEmpty()) {
        item("category_openconnect_authentication") {
            PreferenceCategory(text = { Text(stringResource(Res.string.openconnect_authentication)) })
        }
        preferenceGroup {
            Preference(
                title = { Text(stringResource(Res.string.clear_remembered_authentication_answers)) },
                icon = { MaskedIcon(Res.drawable.delete, IconMaskColors.IconCoral) },
                onClick = viewModel::clearFormEntries,
            )
        }
    }
    item("category_proxy") { PreferenceCategory(text = { Text(stringResource(Res.string.proxy_cat)) }) }
    preferenceGroup {
        TextFieldPreference(
            value = state.server,
            onValueChange = viewModel::setServer,
            title = { Text(stringResource(Res.string.vpn_server_url)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.server)) },
            icon = { MaskedIcon(Res.drawable.router, IconMaskColors.IconLightBlue) },
        )
        ListPreference(
            value = state.flavor,
            onValueChange = viewModel::setFlavor,
            values = flavors,
            title = { Text(stringResource(Res.string.openconnect_flavor)) },
            summary = {
                Text(state.flavor.ifBlank { stringResource(Res.string.auto) })
            },
            icon = { MaskedIcon(Res.drawable.category, IconMaskColors.IconLavender) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it.ifBlank { stringResource(Res.string.auto) }) },
        )
        if (state.flavor == "pulse") {
            ListPreference(
                value = state.reportedOS,
                onValueChange = viewModel::setReportedOS,
                values = pulseReportedOS,
                title = { Text(stringResource(Res.string.openconnect_reported_os)) },
                summary = {
                    Text(state.reportedOS.ifBlank { stringResource(Res.string.auto) })
                },
                icon = { MaskedIcon(Res.drawable.android, IconMaskColors.IconLightYellow) },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = { AnnotatedString(it.ifBlank { stringResource(Res.string.auto) }) },
            )
        }
        TextFieldPreference(
            value = state.username,
            onValueChange = viewModel::setUsername,
            title = { Text(stringResource(Res.string.username)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.username)) },
            icon = { MaskedIcon(Res.drawable.person, IconMaskColors.IconLightGreen) },
        )
        PasswordPreference(
            value = state.password,
            onValueChange = viewModel::setPassword,
            title = { Text(stringResource(Res.string.password)) },
        )
        TextFieldPreference(
            value = state.authGroup,
            onValueChange = viewModel::setAuthGroup,
            title = { Text(stringResource(Res.string.openconnect_auth_group)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.authGroup)) },
            icon = { MaskedIcon(Res.drawable.domain, IconMaskColors.IconLightOrange) },
        )
        TextFieldPreference(
            value = state.userAgent,
            onValueChange = viewModel::setUserAgent,
            title = { Text(stringResource(Res.string.user_agent)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.userAgent)) },
            icon = { MaskedIcon(Res.drawable.language, IconMaskColors.IconWarmGray) },
        )
        TextFieldPreference(
            value = state.localHostname,
            onValueChange = viewModel::setLocalHostname,
            title = { Text(stringResource(Res.string.local_hostname)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.localHostname)) },
            icon = { MaskedIcon(Res.drawable.dns, IconMaskColors.IconWarmGray) },
        )
    }
    item("category_tls") { PreferenceCategory(text = { Text(stringResource(Res.string.security_settings)) }) }
    preferenceGroup {
        SwitchPreference(
            value = state.tlsInsecure,
            onValueChange = viewModel::setTlsInsecure,
            title = { Text(stringResource(Res.string.insecure)) },
            icon = {
                MaskedIcon(
                    Res.drawable.warning_amber,
                    IconMaskColors.IconCoral,
                    IconMaskShapes.risk(),
                )
            },
        )
        TextFieldPreference(
            value = state.tlsServerName,
            onValueChange = viewModel::setTlsServerName,
            title = { Text(stringResource(Res.string.tls_server_name)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.tlsServerName)) },
            icon = { MaskedIcon(Res.drawable.dns, IconMaskColors.IconLightBlue) },
        )
        TextFieldPreference(
            value = state.tlsPeerFingerprint,
            onValueChange = viewModel::setTlsPeerFingerprint,
            title = { Text(stringResource(Res.string.tls_peer_fingerprint)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.tlsPeerFingerprint)) },
            icon = { MaskedIcon(Res.drawable.fingerprint, IconMaskColors.IconLightYellow) },
        )
        TextFieldPreference(
            value = state.certificateAuthority,
            onValueChange = viewModel::setCertificateAuthority,
            title = { Text(stringResource(Res.string.certificate_authority)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.certificateAuthority)) },
            textField = { fieldValue, change, ok ->
                MultilineTextField(fieldValue, change, ok)
            },
            icon = { MaskedIcon(Res.drawable.enhanced_encryption, IconMaskColors.IconLightBlue) },
        )
        TextFieldPreference(
            value = state.clientCertificate,
            onValueChange = viewModel::setClientCertificate,
            title = { Text(stringResource(Res.string.client_certificate)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.clientCertificate)) },
            textField = { fieldValue, change, ok ->
                MultilineTextField(fieldValue, change, ok)
            },
            icon = { MaskedIcon(Res.drawable.fingerprint, IconMaskColors.IconLavender, IconMaskShapes.credential()) },
        )
        TextFieldPreference(
            value = state.clientKey,
            onValueChange = viewModel::setClientKey,
            title = { Text(stringResource(Res.string.client_key)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.clientKey)) },
            textField = { fieldValue, change, ok ->
                MultilineTextField(fieldValue, change, ok)
            },
            icon = { MaskedIcon(Res.drawable.vpn_key, IconMaskColors.IconCoral, IconMaskShapes.credential()) },
        )
        PasswordPreference(
            value = state.clientKeyPassword,
            onValueChange = viewModel::setClientKeyPassword,
        )
        TextFieldPreference(
            value = state.mcaCertificate,
            onValueChange = viewModel::setMcaCertificate,
            title = { Text(stringResource(Res.string.machine_certificate)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.mcaCertificate)) },
            textField = { fieldValue, change, ok ->
                MultilineTextField(fieldValue, change, ok)
            },
            icon = { MaskedIcon(Res.drawable.lock, IconMaskColors.IconLightOrange, IconMaskShapes.credential()) },
        )
        TextFieldPreference(
            value = state.mcaKey,
            onValueChange = viewModel::setMcaKey,
            title = { Text(stringResource(Res.string.machine_key)) },
            textToValue = { it },
            valueToText = { it },
            summary = { Text(contentOrUnset(state.mcaKey)) },
            textField = { fieldValue, change, ok ->
                MultilineTextField(fieldValue, change, ok)
            },
            icon = { MaskedIcon(Res.drawable.vpn_key, IconMaskColors.IconLightPink, IconMaskShapes.credential()) },
        )
        PasswordPreference(
            value = state.mcaKeyPassword,
            onValueChange = viewModel::setMcaKeyPassword,
        )
        SwitchPreference(
            value = state.allowInsecureCrypto,
            onValueChange = viewModel::setAllowInsecureCrypto,
            title = { Text(stringResource(Res.string.openconnect_allow_insecure_crypto)) },
            icon = {
                MaskedIcon(
                    Res.drawable.warning_amber,
                    IconMaskColors.IconCoral,
                    IconMaskShapes.risk(),
                )
            },
        )
    }
}
