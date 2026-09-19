package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.fmt.snell.SnellBean
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.enhanced_encryption
import io.throneproj.thronem.resources.grid_3x3
import io.throneproj.thronem.resources.http_host
import io.throneproj.thronem.resources.obfs_mode
import io.throneproj.thronem.resources.password
import io.throneproj.thronem.resources.pre_shared_key
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.protocol_version
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.settings
import io.throneproj.thronem.resources.snell_mode
import io.throneproj.thronem.resources.snell_reuse
import io.throneproj.thronem.resources.snell_user_key
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun SnellSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: SnellSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        SnellSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        snellSettings(uiState as SnellUiState, viewModel)
    }
}

private fun LazyListScope.snellSettings(
    uiState: SnellUiState,
    viewModel: SnellSettingsViewModel,
) {
    val versions = listOf(SnellBean.VERSION_4, SnellBean.VERSION_6)
    fun versionText(version: Int) = when (version) {
        SnellBean.VERSION_4 -> "v4 (5)"
        else -> "v$version"
    }

    val obfsModes = listOf("", "http", "tls")
    val snellModes = listOf("default", "unshaped", "unsafe-raw")
    fun snellModeText(mode: String) = mode.ifBlank { "default" }

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
        ListPreference(
            value = uiState.version,
            values = versions,
            onValueChange = { viewModel.setVersion(it) },
            title = { Text(stringResource(Res.string.protocol_version)) },
            icon = {
                MaskedIcon(
                    Res.drawable.security,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(versionText(uiState.version)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(versionText(it)) },
        )
        PasswordPreference(
            value = uiState.psk,
            onValueChange = { viewModel.setPsk(it) },
            title = { Text(stringResource(Res.string.pre_shared_key)) },
            icon = {
                MaskedIcon(
                    Res.drawable.password,
                    color = IconMaskColors.IconWarmGray,
                )
            },
        )
        PasswordPreference(
            value = uiState.userKey,
            onValueChange = { viewModel.setUserKey(it) },
            title = { Text(stringResource(Res.string.snell_user_key)) },
            icon = {
                MaskedIcon(
                    Res.drawable.enhanced_encryption,
                    color = IconMaskColors.IconCoral,
                )
            },
        )
    }

    item("category_options") {
        PreferenceCategory(text = { Text(stringResource(Res.string.settings)) })
    }
    preferenceGroup {
        SwitchPreference(
            value = uiState.reuse,
            onValueChange = { viewModel.setReuse(it) },
            title = { Text(stringResource(Res.string.snell_reuse)) },
            icon = {
                MaskedIcon(
                    Res.drawable.grid_3x3,
                    color = IconMaskColors.IconLightBlue,
                )
            },
        )
    }

    when (uiState.version) {
        SnellBean.VERSION_4 -> {
            preferenceGroup {
                ListPreference(
                    value = uiState.obfsMode,
                    values = obfsModes,
                    onValueChange = { viewModel.setObfsMode(it) },
                    title = { Text(stringResource(Res.string.obfs_mode)) },
                    icon = {
                        MaskedIcon(
                            Res.drawable.settings,
                            color = IconMaskColors.IconLightGreen,
                        )
                    },
                    summary = { Text(contentOrUnset(uiState.obfsMode)) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(contentOrUnset(it)) },
                )
                TextFieldPreference(
                    value = uiState.obfsHost,
                    onValueChange = { viewModel.setObfsHost(it) },
                    title = { Text(stringResource(Res.string.http_host)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.router,
                            color = IconMaskColors.IconLightOrange,
                        )
                    },
                    summary = { Text(contentOrUnset(uiState.obfsHost)) },
                    valueToText = { it },
                )
            }
        }

        SnellBean.VERSION_6 -> {
            preferenceGroup {
                ListPreference(
                    value = snellModeText(uiState.mode),
                    values = snellModes,
                    onValueChange = { viewModel.setMode(it) },
                    title = { Text(stringResource(Res.string.snell_mode)) },
                    icon = {
                        MaskedIcon(
                            Res.drawable.settings,
                            color = IconMaskColors.IconLightGreen,
                        )
                    },
                    summary = { Text(snellModeText(uiState.mode)) },
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(it) },
                )
            }
        }
    }

}
