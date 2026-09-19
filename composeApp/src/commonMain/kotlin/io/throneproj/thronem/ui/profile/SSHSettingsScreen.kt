package io.throneproj.thronem.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
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
import io.throneproj.thronem.compose.PreferenceGroupDefaults
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.fmt.ssh.SSHBean
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.compare_arrows
import io.throneproj.thronem.resources.copyright
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.hysteria_auth_type
import io.throneproj.thronem.resources.password
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_cat
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.server_address
import io.throneproj.thronem.resources.server_port
import io.throneproj.thronem.resources.ssh_auth_type_none
import io.throneproj.thronem.resources.ssh_private_key
import io.throneproj.thronem.resources.ssh_private_key_passphrase
import io.throneproj.thronem.resources.ssh_public_key
import io.throneproj.thronem.resources.username
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.ui.NavRoutes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun SSHSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: SSHSettingsViewModel =
        profileEditorViewModel(profileId = profileId, isSubscription = isSubscription) {
            SSHSettingsViewModel()
        }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, _ ->
        sshSettings(uiState as SshUiState, viewModel)
    }
}

private fun LazyListScope.sshSettings(uiState: SshUiState, viewModel: SSHSettingsViewModel) {
    item("category_proxy") {
        PreferenceCategory(text = { Text(stringResource(Res.string.proxy_cat)) })
    }
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
            textToValue = { it.toIntOrNull() ?: 22 },
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
            value = uiState.username,
            onValueChange = { viewModel.setUsername(it) },
            title = { Text(stringResource(Res.string.username)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.person,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(contentOrUnset(uiState.username)) },
            valueToText = { it },
        )
        fun authType(type: Int) =
            when (type) {
                SSHBean.AUTH_TYPE_NONE -> Res.string.ssh_auth_type_none
                SSHBean.AUTH_TYPE_PASSWORD -> Res.string.password
                SSHBean.AUTH_TYPE_PRIVATE_KEY -> Res.string.ssh_public_key
                else -> error("impossible")
            }
        ListPreference(
            value = uiState.authType,
            values =
                listOf(
                    SSHBean.AUTH_TYPE_NONE,
                    SSHBean.AUTH_TYPE_PASSWORD,
                    SSHBean.AUTH_TYPE_PRIVATE_KEY,
                ),
            onValueChange = { viewModel.setAuthType(it) },
            title = { Text(stringResource(Res.string.hysteria_auth_type)) },
            icon = {
                MaskedIcon(
                    Res.drawable.compare_arrows,
                    color = IconMaskColors.IconLightGreen,
                )
            },
            summary = {
                val text = stringResource(authType(uiState.authType))
                Text(text)
            },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(stringResource(authType(it))) },
        )
        AnimatedVisibility(visible = uiState.authType == SSHBean.AUTH_TYPE_PASSWORD) {
            Column(verticalArrangement = PreferenceGroupDefaults.itemArrangement) {
                PasswordPreference(
                    value = uiState.password,
                    onValueChange = { viewModel.setPassword(it) },
                )
            }
        }
        AnimatedVisibility(visible = uiState.authType == SSHBean.AUTH_TYPE_PRIVATE_KEY) {
            Column(verticalArrangement = PreferenceGroupDefaults.itemArrangement) {
                TextFieldPreference(
                    value = uiState.privateKey,
                    onValueChange = { viewModel.setPrivateKey(it) },
                    title = { Text(stringResource(Res.string.ssh_private_key)) },
                    textToValue = { it },
                    icon = {
                        MaskedIcon(
                            Res.drawable.vpn_key,
                            color = IconMaskColors.IconCyan,
                            shape = IconMaskShapes.credential(),
                        )
                    },
                    summary = { Text(contentOrUnset(uiState.privateKey)) },
                    valueToText = { it },
                    textField = { value, onValueChange, onOk ->
                        MultilineTextField(value, onValueChange, onOk)
                    },
                )
                PasswordPreference(
                    value = uiState.privateKeyPassphrase,
                    onValueChange = { viewModel.setPrivateKeyPassphrase(it) },
                    title = { Text(stringResource(Res.string.ssh_private_key_passphrase)) },
                )
            }
        }
        TextFieldPreference(
            value = uiState.publicKey,
            onValueChange = { viewModel.setPublicKey(it) },
            title = { Text(stringResource(Res.string.ssh_public_key)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.copyright,
                    color = IconMaskColors.IconWarmGray,
                )
            },
            summary = { Text(contentOrUnset(uiState.publicKey)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
    }
}
