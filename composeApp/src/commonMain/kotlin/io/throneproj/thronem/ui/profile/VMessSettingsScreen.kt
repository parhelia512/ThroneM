package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.ktx.intListN
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.alter_id
import io.throneproj.thronem.resources.alternate_email
import io.throneproj.thronem.resources.authenticated_length
import io.throneproj.thronem.resources.encryption
import io.throneproj.thronem.resources.enhanced_encryption
import io.throneproj.thronem.resources.experimental_authenticated_length
import io.throneproj.thronem.resources.experimental_settings
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.outbox
import io.throneproj.thronem.resources.packet_encoding
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.uuid
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.StringOrRes
import io.throneproj.thronem.ui.stringOrRes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun VMessSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: VMessSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        VMessSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, scrollTo ->
        vmessSettings(uiState as VMessUiState, viewModel, scrollTo)
    }
}

private fun LazyListScope.vmessSettings(
    uiState: VMessUiState,
    viewModel: VMessSettingsViewModel,
    scrollTo: (String) -> Unit,
) {
    headSettings(uiState, viewModel)
    preferenceGroup {
        TextFieldPreference(
            value = uiState.uuid,
            onValueChange = { viewModel.setUUID(it) },
            title = { Text(stringResource(Res.string.uuid)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.person, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.uuid)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.alterID,
            onValueChange = { viewModel.setAlterID(it) },
            title = { Text(stringResource(Res.string.alter_id)) },
            textToValue = { it.toIntOrNull() ?: 0 },
            icon = {
                MaskedIcon(
                    Res.drawable.alternate_email,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.alterID)) },
            valueToText = { it.toString() },
            textField = { value, onValueChange, onOk ->
                UIntegerTextField(value, onValueChange, onOk)
            },
        )
        ListPreference(
            value = uiState.encryption,
            onValueChange = { viewModel.setEncryption(it) },
            values = listOf("auto", "aes-128-gcm", "chacha20-poly1305", "none", "zero"),
            title = { Text(stringResource(Res.string.encryption)) },
            icon = {
                MaskedIcon(
                    Res.drawable.enhanced_encryption,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(uiState.encryption)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
        fun packetEncodingName(packetEncoding: Int): StringOrRes = when (packetEncoding) {
            0 -> StringOrRes.Res(Res.string.not_set)
            1 -> StringOrRes.Direct("packetaddr")
            2 -> StringOrRes.Direct("XUDP")
            else -> error("impossible")
        }
        ListPreference(
            value = uiState.packetEncoding,
            onValueChange = { viewModel.setPacketEncoding(it) },
            values = intListN(3),
            title = { Text(stringResource(Res.string.packet_encoding)) },
            icon = {
                MaskedIcon(
                    Res.drawable.outbox,
                    color = IconMaskColors.IconLavender,
                )
            },
            summary = { Text(stringOrRes(packetEncodingName(uiState.packetEncoding))) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(stringOrRes(packetEncodingName(it))) },
        )
    }

    transportSettings(uiState, viewModel)
    muxSettings(uiState, viewModel)
    tlsSettings(uiState, viewModel, scrollTo)

    item("category_experimental") {
        PreferenceCategory(
            text = { Text(stringResource(Res.string.experimental_settings)) },
        )
    }
    preferenceGroup(key = "authenticated_length") {
        SwitchPreference(
            value = uiState.authenticatedLength,
            onValueChange = { viewModel.setAuthenticatedLength(it) },
            title = { Text(stringResource(Res.string.authenticated_length)) },
            icon = {
                MaskedIcon(Res.drawable.security, color = IconMaskColors.IconCoral)
            },
            summary = { Text(stringResource(Res.string.experimental_authenticated_length)) },
        )
    }
}
