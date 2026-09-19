package io.throneproj.thronem.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.throneproj.thronem.compose.DurationTextField
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.PortTextField
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.enable_ntp
import io.throneproj.thronem.resources.flip_camera_android
import io.throneproj.thronem.resources.ntp_server_address
import io.throneproj.thronem.resources.ntp_server_port
import io.throneproj.thronem.resources.ntp_sum
import io.throneproj.thronem.resources.ntp_sync_interval
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.timelapse
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NtpSettingsGroup(
    needReload: () -> Unit,
) {
    val enableNtpValue by DataStore.ntpEnable.collectAsStateWithLifecycle()
    SwitchPreference(
        value = enableNtpValue,
        onValueChange = {
            DataStore.ntpEnable.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.enable_ntp)) },
        icon = {
            MaskedIcon(
                Res.drawable.timelapse,
                color = IconMaskColors.IconLightPink,
            )
        },
        summary = { Text(stringResource(Res.string.ntp_sum)) },
    )

    val ntpServerValue by DataStore.ntpAddress.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = ntpServerValue,
        onValueChange = {
            DataStore.ntpAddress.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.ntp_server_address)) },
        textToValue = { it },
        icon = {
            MaskedIcon(Res.drawable.router, color = IconMaskColors.IconLightBlue)
        },
        summary = { Text(contentOrUnset(ntpServerValue)) },
        valueToText = { it },
        enabled = enableNtpValue,
    )

    val ntpPortValue by DataStore.ntpPort.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = ntpPortValue,
        onValueChange = {
            DataStore.ntpPort.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.ntp_server_port)) },
        textToValue = { it.toIntOrNull() ?: 123 },
        icon = {
            MaskedIcon(
                Res.drawable.directions_boat,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(ntpPortValue.toString()) },
        valueToText = { it.toString() },
        enabled = enableNtpValue,
    ) { value, onValueChange, onOk ->
        PortTextField(value, onValueChange, onOk)
    }

    val ntpIntervalValue by DataStore.ntpInterval.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = ntpIntervalValue,
        onValueChange = {
            DataStore.ntpInterval.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.ntp_sync_interval)) },
        textToValue = { it },
        icon = {
            MaskedIcon(
                Res.drawable.flip_camera_android,
                color = IconMaskColors.IconCyan,
            )
        },
        summary = { Text(contentOrUnset(ntpIntervalValue)) },
        valueToText = { it },
        enabled = enableNtpValue,
    ) { value, onValueChange, onOk ->
        DurationTextField(value, onValueChange, onOk)
    }
}
