package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
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
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.lock_open
import io.throneproj.thronem.resources.password
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.pinned_peer_certificate_chain_sha256
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.push_pin
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.security_settings
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.sni
import io.throneproj.thronem.resources.uuid
import io.throneproj.thronem.ui.NavRoutes
import org.jetbrains.compose.resources.stringResource

@Composable
fun JuicitySettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: JuicitySettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        JuicitySettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        juicitySettings(uiState as JuicityUiState, viewModel)
    }
}

private fun LazyListScope.juicitySettings(
    uiState: JuicityUiState,
    viewModel: JuicitySettingsViewModel,
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
        TextFieldPreference(
            value = uiState.uuid,
            onValueChange = { viewModel.setUuid(it) },
            title = { Text(stringResource(Res.string.uuid)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.person,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.uuid)) },
            valueToText = { it },
        )
        PasswordPreference(
            value = uiState.password,
            onValueChange = { viewModel.setPassword(it) },
            icon = {
                MaskedIcon(
                    Res.drawable.password,
                    color = IconMaskColors.IconWarmGray,
                )
            },
        )
    }

    item("category_tls") {
        PreferenceCategory(text = { Text(stringResource(Res.string.security_settings)) })
    }
    preferenceGroup {
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
            value = uiState.allowInsecure,
            onValueChange = { viewModel.setAllowInsecure(it) },
            title = { Text(stringResource(Res.string.allow_insecure)) },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.lock_open,
                    color = IconMaskColors.IconCoral,
                    shape = IconMaskShapes.risk(),
                )
            },
        )
        TextFieldPreference(
            value = uiState.pinSha256,
            onValueChange = { viewModel.setPinSha256(it) },
            title = { Text(stringResource(Res.string.pinned_peer_certificate_chain_sha256)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.push_pin,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(uiState.pinSha256)) },
            valueToText = { it },
        )
    }
}
