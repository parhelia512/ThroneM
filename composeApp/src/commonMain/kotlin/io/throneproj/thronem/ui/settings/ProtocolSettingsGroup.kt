package io.throneproj.thronem.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.download
import io.throneproj.thronem.resources.file_upload
import io.throneproj.thronem.resources.hysteria_download_mbps
import io.throneproj.thronem.resources.hysteria_upload_mbps
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProtocolSettingsGroup(
    needReload: () -> Unit,
) {
    val uploadSpeedValue by DataStore.uploadSpeed.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = uploadSpeedValue,
        onValueChange = {
            DataStore.uploadSpeed.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.hysteria_upload_mbps)) },
        textToValue = { it.toIntOrNull() ?: 0 },
        icon = {
            MaskedIcon(
                Res.drawable.file_upload,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(uploadSpeedValue.toString()) },
        valueToText = { it.toString() },
    ) { value, onValueChange, onOk ->
        UIntegerTextField(value, onValueChange, onOk)
    }

    val downloadSpeedValue by DataStore.downloadSpeed.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = downloadSpeedValue,
        onValueChange = {
            DataStore.downloadSpeed.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.hysteria_download_mbps)) },
        textToValue = { it.toIntOrNull() ?: 0 },
        icon = {
            MaskedIcon(Res.drawable.download, color = IconMaskColors.IconLightBlue)
        },
        summary = { Text(downloadSpeedValue.toString()) },
        valueToText = { it.toString() },
    ) { value, onValueChange, onOk ->
        UIntegerTextField(value, onValueChange, onOk)
    }
}
