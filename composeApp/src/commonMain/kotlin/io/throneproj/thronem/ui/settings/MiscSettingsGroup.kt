package io.throneproj.thronem.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.LinkOrContentTextField
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.SliderPreference
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apps
import io.throneproj.thronem.resources.cast_connected
import io.throneproj.thronem.resources.cert_chrome
import io.throneproj.thronem.resources.certificate_authority
import io.throneproj.thronem.resources.connection_test_ignore_handshake_time
import io.throneproj.thronem.resources.connection_test_unified_delay
import io.throneproj.thronem.resources.connection_test_url
import io.throneproj.thronem.resources.fast_forward
import io.throneproj.thronem.resources.follow_system
import io.throneproj.thronem.resources.mozilla
import io.throneproj.thronem.resources.push_pin
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.system_and_user
import io.throneproj.thronem.resources.test_concurrency
import io.throneproj.thronem.resources.test_timeout
import io.throneproj.thronem.resources.timer
import io.throneproj.thronem.ui.DisableProcessTextPreference
import io.throneproj.thronem.ui.EnableTaskerPreference
import io.throneproj.thronem.ui.HideLauncherIconPreference
import io.throneproj.thronem.ui.PlatformMiscOptions
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MiscSettingsGroup(
    needReload: () -> Unit,
    needRestart: () -> Unit,
) {
    val connectionTestUrlValue by DataStore.connectionTestURL.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = connectionTestUrlValue,
        onValueChange = { DataStore.connectionTestURL.setBlocking(it) },
        title = { Text(stringResource(Res.string.connection_test_url)) },
        textToValue = { it },
        icon = {
            MaskedIcon(
                Res.drawable.cast_connected,
                color = IconMaskColors.IconCyan,
            )
        },
        summary = { Text(contentOrUnset(connectionTestUrlValue)) },
        valueToText = { it },
    ) { value, onValueChange, onOk ->
        LinkOrContentTextField(value, onValueChange, onOk)
    }

    val connectionTestConcurrentValue by DataStore.connectionTestConcurrent.collectAsStateWithLifecycle()
    var concurrentPreview by remember { mutableFloatStateOf(connectionTestConcurrentValue.toFloat()) }
    SliderPreference(
        value = connectionTestConcurrentValue.toFloat(),
        onValueChange = { DataStore.connectionTestConcurrent.setBlocking(it.toInt()) },
        sliderValue = concurrentPreview,
        onSliderValueChange = { concurrentPreview = it },
        title = { Text(stringResource(Res.string.test_concurrency)) },
        valueRange = 1f..32f,
        valueSteps = 32,
        icon = {
            MaskedIcon(
                Res.drawable.fast_forward,
                color = IconMaskColors.IconLightGreen,
            )
        },
        valueText = { Text(concurrentPreview.toInt().toString()) },
    )

    val connectionTestTimeoutValue by DataStore.connectionTestTimeout.collectAsStateWithLifecycle()
    var timeoutPreview by remember { mutableFloatStateOf(connectionTestTimeoutValue.toFloat()) }
    SliderPreference(
        value = connectionTestTimeoutValue.toFloat(),
        onValueChange = { DataStore.connectionTestTimeout.setBlocking(it.toInt()) },
        sliderValue = timeoutPreview,
        onSliderValueChange = { timeoutPreview = it },
        title = { Text(stringResource(Res.string.test_timeout)) },
        valueRange = 1024f..8192f,
        valueSteps = 20,
        icon = {
            MaskedIcon(Res.drawable.apps, color = IconMaskColors.IconWarmGray)
        },
        valueText = { Text(timeoutPreview.toInt().toString()) },
    )

    val connectionTestUnifiedDelay by DataStore.connectionTestUnifiedDelay.collectAsStateWithLifecycle()
    SwitchPreference(
        value = connectionTestUnifiedDelay,
        onValueChange = {
            DataStore.connectionTestUnifiedDelay.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.connection_test_unified_delay)) },
        icon = {
            MaskedIcon(Res.drawable.timer, IconMaskColors.IconLightGreen)
        },
    )

    val connectionTestIgnoreHandshakeTime by DataStore.connectionTestIgnoreHandshakeTime.collectAsStateWithLifecycle()
    SwitchPreference(
        value = connectionTestIgnoreHandshakeTime,
        onValueChange = {
            DataStore.connectionTestIgnoreHandshakeTime.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.connection_test_ignore_handshake_time)) },
        icon = {
            MaskedIcon(Res.drawable.question_mark, IconMaskColors.IconLightGreen)
        },
    )
    PlatformMiscOptions(needReload)

    // The global CA-pool switch is gone with libcore: libbox always uses the
    // system trust store, and the Kotlin HTTP fetcher cannot override it.

    DisableProcessTextPreference()
    EnableTaskerPreference()
    HideLauncherIconPreference()
}
