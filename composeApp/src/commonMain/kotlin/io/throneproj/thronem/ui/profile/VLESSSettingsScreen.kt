package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.ktx.intListN
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.encrypted
import io.throneproj.thronem.resources.encryption
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.outbox
import io.throneproj.thronem.resources.packet_encoding
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.stream
import io.throneproj.thronem.resources.uuid
import io.throneproj.thronem.resources.xtls_flow
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.StringOrRes
import io.throneproj.thronem.ui.stringOrRes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.stringResource

@Composable
fun VLESSSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: VLESSSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        VLESSSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, scrollTo ->
        vlessSettings(uiState as VLESSUiState, viewModel, scrollTo)
    }
}

private fun LazyListScope.vlessSettings(
    uiState: VLESSUiState,
    viewModel: VLESSSettingsViewModel,
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
        ListPreference(
            value = uiState.flow,
            onValueChange = { viewModel.setFlow(it) },
            values = listOf("", "xtls-rprx-vision"),
            title = { Text(stringResource(Res.string.xtls_flow)) },
            icon = {
                MaskedIcon(
                    Res.drawable.stream,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.flow)) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(it) },
        )
        TextFieldPreference(
            value = uiState.encryption,
            onValueChange = { viewModel.setEncryption(it) },
            title = { Text(stringResource(Res.string.encryption)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    resource = Res.drawable.encrypted,
                    color = IconMaskColors.IconCoral,
                )
            },
            summary = { Text(contentOrUnset(uiState.encryption)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
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
}
