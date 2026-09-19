package io.throneproj.thronem.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.AnnotatedString
import io.throneproj.thronem.Key
import io.throneproj.thronem.NetworkInterfaceStrategy
import io.throneproj.thronem.RuleProvider
import io.throneproj.thronem.compose.DurationTextField
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.LinkOrContentTextField
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultiSelectListPreference
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.auto
import io.throneproj.thronem.resources.construction
import io.throneproj.thronem.resources.custom_rule_provider
import io.throneproj.thronem.resources.disable_tcp_keep_alive
import io.throneproj.thronem.resources.ecg
import io.throneproj.thronem.resources.fallback
import io.throneproj.thronem.resources.hourglass_top
import io.throneproj.thronem.resources.hybrid
import io.throneproj.thronem.resources.import_contacts
import io.throneproj.thronem.resources.ipv4_only
import io.throneproj.thronem.resources.ipv6_only
import io.throneproj.thronem.resources.keep_default
import io.throneproj.thronem.resources.network_interface_preference
import io.throneproj.thronem.resources.network_interface_strategy
import io.throneproj.thronem.resources.network_strategy
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.prefer_ipv4
import io.throneproj.thronem.resources.prefer_ipv6
import io.throneproj.thronem.resources.public_icon
import io.throneproj.thronem.resources.route_rules_official
import io.throneproj.thronem.resources.route_rules_provider
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.rule_folder
import io.throneproj.thronem.resources.tcp_keep_alive_idle
import io.throneproj.thronem.resources.tcp_keep_alive_interval
import io.throneproj.thronem.resources.timer
import io.throneproj.thronem.ui.PlatformRouteOptions
import io.throneproj.thronem.ui.ProxyAppsPreferences
import io.throneproj.thronem.ui.StringOrRes
import io.throneproj.thronem.ui.stringOrRes
import me.zhanghai.compose.preference.ListPreferenceType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RouteSettingsGroup(
    needReload: () -> Unit,
    openAppManager: () -> Unit,
) {
    val serviceModeState by DataStore.serviceMode.collectAsStateWithLifecycle()

    ProxyAppsPreferences(openAppManager)

    PlatformRouteOptions(
        needReload = needReload,
        isVpnMode = serviceModeState == Key.MODE_VPN,
    )

    fun networkStrategyTextRes(value: String): StringResource = when (value) {
        "" -> Res.string.auto
        "prefer_ipv6" -> Res.string.prefer_ipv6
        "prefer_ipv4" -> Res.string.prefer_ipv4
        "ipv4_only" -> Res.string.ipv4_only
        "ipv6_only" -> Res.string.ipv6_only
        else -> Res.string.auto
    }

    val networkStrategyValue by DataStore.networkStrategy.collectAsStateWithLifecycle()
    ListPreference(
        value = networkStrategyValue,
        onValueChange = {
            DataStore.networkStrategy.setBlocking(it)
            needReload()
        },
        values = listOf("", "prefer_ipv6", "prefer_ipv4", "ipv4_only", "ipv6_only"),
        title = { Text(stringResource(Res.string.network_strategy)) },
        icon = {
            MaskedIcon(Res.drawable.router, color = IconMaskColors.IconLightBlue)
        },
        summary = { Text(stringResource(networkStrategyTextRes(networkStrategyValue))) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = { AnnotatedString(stringResource(networkStrategyTextRes(it))) },
    )

    fun networkInterfaceStrategyTextRes(selection: Int): StringResource = when (selection) {
        NetworkInterfaceStrategy.DEFAULT -> Res.string.keep_default
        NetworkInterfaceStrategy.HYBRID -> Res.string.hybrid
        NetworkInterfaceStrategy.FALLBACK -> Res.string.fallback
        else -> Res.string.keep_default
    }

    val networkInterfaceValue by DataStore.networkInterfaceType.collectAsStateWithLifecycle()
    ListPreference(
        value = networkInterfaceValue,
        onValueChange = {
            DataStore.networkInterfaceType.setBlocking(it)
            needReload()
        },
        values = listOf(
            NetworkInterfaceStrategy.DEFAULT,
            NetworkInterfaceStrategy.HYBRID,
            NetworkInterfaceStrategy.FALLBACK,
        ),
        title = { Text(stringResource(Res.string.network_interface_strategy)) },
        icon = {
            MaskedIcon(
                Res.drawable.construction,
                color = IconMaskColors.IconWarmGray,
            )
        },
        summary = { Text(stringResource(networkInterfaceStrategyTextRes(networkInterfaceValue))) },
        type = ListPreferenceType.DROPDOWN_MENU,
        valueToText = { AnnotatedString(stringResource(networkInterfaceStrategyTextRes(it))) },
    )

    val preferredInterfaces by DataStore.networkPreferredInterfaces.collectAsStateWithLifecycle()
    MultiSelectListPreference(
        value = preferredInterfaces,
        onValueChange = {
            DataStore.networkPreferredInterfaces.setBlocking(it)
            needReload()
        },
        values = listOf("wifi", "cellular", "ethernet", "other"),
        title = { Text(stringResource(Res.string.network_interface_preference)) },
        icon = {
            MaskedIcon(
                Res.drawable.public_icon,
                color = IconMaskColors.IconWarmGray,
            )
        },
        summary = {
            val text = if (preferredInterfaces.isEmpty()) {
                stringResource(Res.string.not_set)
            } else preferredInterfaces.joinToString("\n")
            Text(text)
        },
        valueToText = { AnnotatedString(it) },
    )

    val disableTcpKeepAliveValue by DataStore.disableTcpKeepAlive.collectAsStateWithLifecycle()
    SwitchPreference(
        value = disableTcpKeepAliveValue,
        onValueChange = {
            DataStore.disableTcpKeepAlive.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.disable_tcp_keep_alive)) },
        icon = {
            MaskedIcon(Res.drawable.ecg, color = IconMaskColors.IconLightGreen)
        },
    )

    val tcpKeepAliveIdleValue by DataStore.tcpKeepAliveIdle.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = tcpKeepAliveIdleValue,
        onValueChange = {
            DataStore.tcpKeepAliveIdle.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.tcp_keep_alive_idle)) },
        textToValue = { it },
        enabled = !disableTcpKeepAliveValue,
        icon = {
            MaskedIcon(
                Res.drawable.hourglass_top,
                color = IconMaskColors.IconWarmGray,
            )
        },
        summary = { Text(contentOrUnset(tcpKeepAliveIdleValue)) },
        valueToText = { it },
    ) { value, onValueChange, onOk ->
        DurationTextField(value, onValueChange, onOk)
    }

    val tcpKeepAliveIntervalValue by DataStore.tcpKeepAliveInterval.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = tcpKeepAliveIntervalValue,
        onValueChange = {
            DataStore.tcpKeepAliveInterval.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.tcp_keep_alive_interval)) },
        textToValue = { it },
        enabled = !disableTcpKeepAliveValue,
        icon = {
            MaskedIcon(Res.drawable.timer, color = IconMaskColors.IconWarmGray)
        },
        summary = { Text(contentOrUnset(tcpKeepAliveIntervalValue)) },
        valueToText = { it },
    ) { value, onValueChange, onOk ->
        DurationTextField(value, onValueChange, onOk)
    }

    fun rulesProviderText(index: Int): StringOrRes = when (index) {
        RuleProvider.OFFICIAL -> StringOrRes.Res(Res.string.route_rules_official)
        RuleProvider.LOYALSOLDIER -> StringOrRes.Direct("Loyalsoldier (1715173329/sing-geo*)")
        RuleProvider.CHOCOLATE4U -> StringOrRes.Direct("Chocolate4U/Iran-sing-box-rules")
        RuleProvider.RUNETFREEDOM -> StringOrRes.Direct("runetfreedom/russia-v2ray-rules-dat")
        RuleProvider.CUSTOM -> StringOrRes.Res(Res.string.custom_rule_provider)
        else -> StringOrRes.Res(Res.string.route_rules_official)
    }

    val rulesProviderValue by DataStore.rulesProvider.collectAsStateWithLifecycle()
    ListPreference(
        value = rulesProviderValue,
        onValueChange = { DataStore.rulesProvider.setBlocking(it) },
        values = listOf(
            RuleProvider.OFFICIAL,
            RuleProvider.LOYALSOLDIER,
            RuleProvider.CHOCOLATE4U,
            RuleProvider.RUNETFREEDOM,
            RuleProvider.CUSTOM,
        ),
        title = { Text(stringResource(Res.string.route_rules_provider)) },
        icon = {
            MaskedIcon(
                Res.drawable.rule_folder,
                color = IconMaskColors.IconLightYellow,
            )
        },
        summary = { Text(stringOrRes(rulesProviderText(rulesProviderValue))) },
        type = ListPreferenceType.ALERT_DIALOG,
        valueToText = { AnnotatedString(stringOrRes(rulesProviderText(it))) },
    )
    if (rulesProviderValue == RuleProvider.CUSTOM) {
        val customRuleProviderValue by DataStore.customRuleProvider.collectAsStateWithLifecycle()
        TextFieldPreference(
            value = customRuleProviderValue,
            onValueChange = { DataStore.customRuleProvider.setBlocking(it) },
            title = { Text(stringResource(Res.string.custom_rule_provider)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.import_contacts,
                    color = IconMaskColors.IconLightYellow,
                )
            },
            summary = { Text(contentOrUnset(customRuleProviderValue)) },
            valueToText = { it },
        ) { value, onValueChange, onOk ->
            LinkOrContentTextField(value, onValueChange, onOk)
        }
    }
}
