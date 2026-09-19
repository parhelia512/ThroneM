package io.throneproj.thronem.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.Key
import io.throneproj.thronem.TunIpStack
import io.throneproj.thronem.bg.BackendState
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.Preference
import io.throneproj.thronem.compose.SliderPreference
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Surface
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.theme.themeString
import io.throneproj.thronem.compose.theme.themes
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.preference.PreferenceProxy
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.ktx.intListN
import io.throneproj.thronem.logLevelString
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.always_show_address
import io.throneproj.thronem.resources.always_show_address_sum
import io.throneproj.thronem.resources.auto
import io.throneproj.thronem.resources.blurred_address
import io.throneproj.thronem.resources.bug_report
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.center_focus_weak
import io.throneproj.thronem.resources.check
import io.throneproj.thronem.resources.color_lens
import io.throneproj.thronem.resources.description
import io.throneproj.thronem.resources.developer_board
import io.throneproj.thronem.resources.developer_mode
import io.throneproj.thronem.resources.disable
import io.throneproj.thronem.resources.enable
import io.throneproj.thronem.resources.flip_camera_android
import io.throneproj.thronem.resources.follow_system
import io.throneproj.thronem.resources.insecure_warn
import io.throneproj.thronem.resources.language
import io.throneproj.thronem.resources.language_system_default
import io.throneproj.thronem.resources.log_level
import io.throneproj.thronem.resources.long_click_to_see_name
import io.throneproj.thronem.resources.max_log_line
import io.throneproj.thronem.resources.mtu
import io.throneproj.thronem.resources.night_mode
import io.throneproj.thronem.resources.profile_traffic_statistics
import io.throneproj.thronem.resources.profile_traffic_statistics_summary
import io.throneproj.thronem.resources.public_icon
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.service_mode
import io.throneproj.thronem.resources.service_mode_proxy
import io.throneproj.thronem.resources.service_mode_vpn
import io.throneproj.thronem.resources.show_direct_speed
import io.throneproj.thronem.resources.show_direct_speed_sum
import io.throneproj.thronem.resources.shutter_speed
import io.throneproj.thronem.resources.speed
import io.throneproj.thronem.resources.speed_interval
import io.throneproj.thronem.resources.theme
import io.throneproj.thronem.resources.traffic
import io.throneproj.thronem.resources.transgender
import io.throneproj.thronem.resources.translate
import io.throneproj.thronem.resources.tun_ip_stack
import io.throneproj.thronem.resources.wb_sunny
import io.throneproj.thronem.ui.AppLanguage
import io.throneproj.thronem.ui.AutoConnectPreference
import io.throneproj.thronem.ui.MeteredNetworkPreference
import io.throneproj.thronem.ui.PlatformGeneralOptions
import io.throneproj.thronem.ui.PlatformSecurityOptions
import io.throneproj.thronem.ui.StringOrRes
import io.throneproj.thronem.ui.getStringOrRes
import io.throneproj.thronem.ui.rememberAppLanguageController
import io.throneproj.thronem.ui.rememberApplyNightMode
import io.throneproj.thronem.ui.rememberThemeExtraColors
import io.throneproj.thronem.ui.stringOrRes
import kotlinx.coroutines.runBlocking
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
private fun ColorPickerPreference(
    proxy: PreferenceProxy<Int>,
    title: @Composable () -> Unit,
    enabled: Boolean = true,
) {
    val currentTheme by proxy.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val extraColors = rememberThemeExtraColors()
    Preference(
        title = { title() },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        icon = {
            MaskedIcon(
                Res.drawable.color_lens,
                color = IconMaskColors.IconLightOrange,
            )
        },
        summary = { Text(stringResource(themeString(currentTheme))) },
        widgetContainer = {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Circle(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val colors = themes + extraColors

        BasicAlertDialog(
            onDismissRequest = { showDialog = false },
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.theme),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (PlatformInfo.isAndroid) Text(
                        text = stringResource(Res.string.long_click_to_see_name),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.labelSmallEmphasized,
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        items(
                            count = colors.size,
                            key = { index -> index },
                            contentType = { 0 },
                        ) { index ->
                            val theme = index + 1
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        proxy.setBlocking(theme)
                                        showDialog = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above,
                                    ),
                                    tooltip = {
                                        PlainTooltip {
                                            Text(stringResource(themeString(theme)))
                                        }
                                    },
                                    state = rememberTooltipState(),
                                ) {
                                    Circle(
                                        modifier = Modifier.size(48.dp),
                                        color = colors[index],
                                        selected = currentTheme == theme,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(stringResource(Res.string.cancel)) {
                            showDialog = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GeneralSettingsGroup(
    needReload: () -> Unit,
    needRestart: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val applyNightMode = rememberApplyNightMode()
    val isExpertState by DataStore.isExpert.collectAsStateWithLifecycle()

    AutoConnectPreference(showMessage = showMessage)

    ColorPickerPreference(
        proxy = DataStore.appTheme,
        title = { Text(stringResource(Res.string.theme)) },
    )

    fun nightString(index: Int): StringResource = when (index) {
        0 -> Res.string.follow_system
        1 -> Res.string.enable
        2 -> Res.string.disable
        3 -> Res.string.auto
        else -> Res.string.follow_system
    }

    val nightValue by DataStore.nightTheme.collectAsStateWithLifecycle()
    ListPreference(
        value = nightValue,
        onValueChange = {
            DataStore.nightTheme.setBlocking(it)
            applyNightMode(it)
        },
        values = intListN(4),
        title = { Text(stringResource(Res.string.night_mode)) },
        icon = {
            MaskedIcon(
                Res.drawable.wb_sunny,
                color = IconMaskColors.IconLightOrange,
            )
        },
        summary = { Text(stringResource(nightString(nightValue))) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = { AnnotatedString(stringResource(nightString(it))) },
    )

    fun getLanguageDisplayName(tag: String): String =
        AppLanguage.fromTag(tag)?.displayName ?: runBlocking {
            resolveRepository().getString(Res.string.language_system_default)
        }

    val languageValues = AppLanguage.entries.map { it.tag }
    val languageController = rememberAppLanguageController(defaultTag = "")
    val appLanguage by languageController.flow.collectAsStateWithLifecycle(languageController.value)
    val selectedLanguage = if (appLanguage in languageValues) appLanguage else ""
    ListPreference(
        value = selectedLanguage,
        onValueChange = { languageController.value = it },
        values = languageValues,
        title = { Text(stringResource(Res.string.language)) },
        icon = {
            MaskedIcon(Res.drawable.translate, color = IconMaskColors.IconLavender)
        },
        summary = { Text(getLanguageDisplayName(selectedLanguage)) },
        type = ListPreferenceType.ALERT_DIALOG,
        valueToText = { AnnotatedString(getLanguageDisplayName(it)) },
    )

    fun serviceModeText(mode: String): StringResource = when (mode) {
        Key.MODE_VPN -> Res.string.service_mode_vpn
        Key.MODE_PROXY -> Res.string.service_mode_proxy
        else -> Res.string.service_mode_vpn
    }

    val serviceModeValue by DataStore.serviceMode.collectAsStateWithLifecycle()
    val serviceStatus by BackendState.status.collectAsStateWithLifecycle()
    ListPreference(
        value = serviceModeValue,
        onValueChange = { mode ->
            DataStore.serviceMode.setBlocking(mode)
            if (serviceStatus.state.canStop) {
                resolveRepository().reloadService()
            }
        },
        values = listOf(Key.MODE_VPN, Key.MODE_PROXY),
        title = { Text(stringResource(Res.string.service_mode)) },
        icon = {
            MaskedIcon(
                Res.drawable.developer_mode,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(stringResource(serviceModeText(serviceModeValue))) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = { AnnotatedString(stringResource(serviceModeText(it))) },
    )

    fun tunIpStackText(value: Int): String = when (value) {
        TunIpStack.GVISOR -> "gVisor"
        TunIpStack.SYSTEM -> "System"
        TunIpStack.MIXED -> "Mixed"
        else -> error("impossible")
    }

    val tunIpStackValue by DataStore.tunIpStack.collectAsStateWithLifecycle()
    ListPreference(
        value = tunIpStackValue,
        onValueChange = {
            DataStore.tunIpStack.setBlocking(it)
            needReload()
        },
        values = listOf(
            TunIpStack.MIXED,
            TunIpStack.GVISOR,
            TunIpStack.SYSTEM,
        ),
        title = { Text(stringResource(Res.string.tun_ip_stack)) },
        icon = {
            MaskedIcon(
                Res.drawable.flip_camera_android,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(tunIpStackText(tunIpStackValue)) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = { AnnotatedString(tunIpStackText(it)) },
    )

    val mtuValue by DataStore.mtu.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = mtuValue,
        onValueChange = {
            DataStore.mtu.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.mtu)) },
        textToValue = { it.toIntOrNull() ?: 9000 },
        icon = {
            MaskedIcon(
                Res.drawable.public_icon,
                color = IconMaskColors.IconLightYellow,
            )
        },
        summary = { Text(mtuValue.toString()) },
        valueToText = { it.toString() },
    )
    PlatformGeneralOptions(needReload)

    fun speedIntervalText(ms: Int): StringOrRes = when (ms) {
        0 -> StringOrRes.Res(Res.string.disable)
        500 -> StringOrRes.Direct("500ms")
        1000 -> StringOrRes.Direct("1s")
        3000 -> StringOrRes.Direct("3s")
        10000 -> StringOrRes.Direct("10s")
        else -> StringOrRes.Direct("1s")
    }

    val speedIntervalValue by DataStore.speedInterval.collectAsStateWithLifecycle()
    ListPreference(
        value = speedIntervalValue,
        onValueChange = { DataStore.speedInterval.setBlocking(it) },
        values = listOf(0, 500, 1000, 3000, 10000),
        title = { Text(stringResource(Res.string.speed_interval)) },
        icon = {
            MaskedIcon(
                Res.drawable.shutter_speed,
                color = IconMaskColors.IconLightPink,
            )
        },
        summary = { Text(stringOrRes(speedIntervalText(speedIntervalValue))) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = {
            val text = runBlocking { getStringOrRes(speedIntervalText(it)) }
            AnnotatedString(text)
        },
    )

    val profileTrafficStatisticsValue by DataStore.profileTrafficStatistics.collectAsStateWithLifecycle()
    SwitchPreference(
        value = profileTrafficStatisticsValue,
        onValueChange = { DataStore.profileTrafficStatistics.setBlocking(it) },
        title = { Text(stringResource(Res.string.profile_traffic_statistics)) },
        icon = {
            MaskedIcon(
                Res.drawable.traffic,
                color = IconMaskColors.IconLightYellow,
            )
        },
        summary = { Text(stringResource(Res.string.profile_traffic_statistics_summary)) },
        enabled = speedIntervalValue != 0,
    )

    val showDirectSpeedValue by DataStore.showDirectSpeed.collectAsStateWithLifecycle()
    SwitchPreference(
        value = showDirectSpeedValue,
        onValueChange = { DataStore.showDirectSpeed.setBlocking(it) },
        title = { Text(stringResource(Res.string.show_direct_speed)) },
        icon = {
            MaskedIcon(Res.drawable.speed, color = IconMaskColors.IconLightPink)
        },
        summary = { Text(stringResource(Res.string.show_direct_speed_sum)) },
        enabled = speedIntervalValue != 0,
    )

    val alwaysShowAddressValue by DataStore.alwaysShowAddress.collectAsStateWithLifecycle()
    SwitchPreference(
        value = alwaysShowAddressValue,
        onValueChange = { DataStore.alwaysShowAddress.setBlocking(it) },
        title = { Text(stringResource(Res.string.always_show_address)) },
        icon = {
            MaskedIcon(
                Res.drawable.center_focus_weak,
                color = IconMaskColors.IconCoral,
            )
        },
        summary = { Text(stringResource(Res.string.always_show_address_sum)) },
    )

    val blurredAddressValue by DataStore.blurredAddress.collectAsStateWithLifecycle()
    SwitchPreference(
        value = blurredAddressValue,
        onValueChange = { DataStore.blurredAddress.setBlocking(it) },
        title = { Text(stringResource(Res.string.blurred_address)) },
        icon = {
            MaskedIcon(
                Res.drawable.transgender,
                color = IconMaskColors.IconLavender,
            )
        },
        enabled = alwaysShowAddressValue,
    )

    val securityAdvisoryValue by DataStore.securityAdvisory.collectAsStateWithLifecycle()
    SwitchPreference(
        value = securityAdvisoryValue,
        onValueChange = { DataStore.securityAdvisory.setBlocking(it) },
        title = { Text(stringResource(Res.string.insecure_warn)) },
        icon = {
            MaskedIcon(
                Res.drawable.security,
                color = IconMaskColors.IconCoral,
                shape = IconMaskShapes.risk(),
            )
        },
    )
    PlatformSecurityOptions()
    MeteredNetworkPreference(needReload)

    val logLevelValue by DataStore.logLevel.collectAsStateWithLifecycle()
    ListPreference(
        value = logLevelValue,
        onValueChange = {
            DataStore.logLevel.setBlocking(it)
            needRestart()
        },
        values = intListN(7),
        title = { Text(stringResource(Res.string.log_level)) },
        icon = {
            MaskedIcon(
                Res.drawable.bug_report,
                color = IconMaskColors.IconLightYellow,
            )
        },
        summary = { Text(logLevelString(logLevelValue)) },
        type = ListPreferenceType.ALERT_DIALOG,
        valueToText = { AnnotatedString(logLevelString(it)) },
    )

    val maxLogLineValue by DataStore.logMaxLine.collectAsStateWithLifecycle()
    var previewValue by remember { mutableFloatStateOf(maxLogLineValue.toFloat()) }
    SliderPreference(
        value = maxLogLineValue.toFloat(),
        onValueChange = { DataStore.logMaxLine.setBlocking(it.toInt()) },
        sliderValue = previewValue,
        onSliderValueChange = { previewValue = it },
        title = { Text(stringResource(Res.string.max_log_line)) },
        valueRange = 1024f..1024f * 64f,
        valueSteps = 128,
        icon = {
            MaskedIcon(
                Res.drawable.description,
                color = IconMaskColors.IconWarmGray,
            )
        },
        valueText = { Text(previewValue.toInt().toString()) },
    )
    if (isExpertState) {
        val debugListenValue by DataStore.debugListen.collectAsStateWithLifecycle()
        TextFieldPreference(
            value = debugListenValue,
            onValueChange = {
                DataStore.debugListen.setBlocking(it)
                needReload()
            },
            title = { Text("pprof listen") },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.developer_board,
                    color = IconMaskColors.IconCoral,
                    shape = IconMaskShapes.risk(),
                )
            },
            summary = { Text(contentOrUnset(debugListenValue)) },
            valueToText = { it },
        )
    }
}

@Composable
private fun Circle(
    modifier: Modifier = Modifier,
    color: Color,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier.background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = vectorResource(Res.drawable.check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
