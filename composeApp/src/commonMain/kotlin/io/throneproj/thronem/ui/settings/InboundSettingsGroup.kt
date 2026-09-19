package io.throneproj.thronem.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.PortTextField
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.allow_access
import io.throneproj.thronem.resources.allow_access_sum
import io.throneproj.thronem.resources.app_registration
import io.throneproj.thronem.resources.append_http_proxy
import io.throneproj.thronem.resources.append_http_proxy_sum
import io.throneproj.thronem.resources.apps
import io.throneproj.thronem.resources.directions_boat
import io.throneproj.thronem.resources.inbound_password
import io.throneproj.thronem.resources.inbound_username
import io.throneproj.thronem.resources.nat
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.port_local_dns
import io.throneproj.thronem.resources.port_proxy
import io.throneproj.thronem.resources.wifi
import io.throneproj.thronem.ui.HttpProxyBypassPreference
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InboundSettingsGroup(
    needReload: () -> Unit,
) {
    val isExpertState by DataStore.isExpert.collectAsStateWithLifecycle()

    val mixedPort by DataStore.mixedPort.collectAsStateWithLifecycle()
    val mixedPortValue = mixedPort.toString()
    TextFieldPreference(
        value = mixedPortValue,
        onValueChange = {
            DataStore.mixedPort.setBlocking(it.toIntOrNull() ?: 2080)
            needReload()
        },
        title = { Text(stringResource(Res.string.port_proxy)) },
        textToValue = { it },
        icon = {
            MaskedIcon(
                Res.drawable.directions_boat,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(contentOrUnset(mixedPortValue)) },
        valueToText = { it },
    ) { value, onValueChange, onOk ->
        PortTextField(value, onValueChange, onOk)
    }

    val localDnsPort by DataStore.localDNSPort.collectAsStateWithLifecycle()
    val localDnsPortValue = localDnsPort.toString()
    TextFieldPreference(
        value = localDnsPortValue,
        onValueChange = {
            DataStore.localDNSPort.setBlocking(it.toIntOrNull() ?: 0)
            needReload()
        },
        title = { Text(stringResource(Res.string.port_local_dns)) },
        textToValue = { it },
        icon = {
            MaskedIcon(Res.drawable.apps, color = IconMaskColors.IconWarmGray)
        },
        summary = { Text(contentOrUnset(localDnsPortValue)) },
        valueToText = { it },
    ) { value, onValueChange, onOk ->
        PortTextField(value, onValueChange, onOk)
    }

    val appendHttpProxyValue by DataStore.appendHttpProxy.collectAsStateWithLifecycle()
    SwitchPreference(
        value = appendHttpProxyValue,
        onValueChange = {
            DataStore.appendHttpProxy.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.append_http_proxy)) },
        icon = {
            MaskedIcon(
                Res.drawable.app_registration,
                color = IconMaskColors.IconLightGreen,
            )
        },
        summary = {
            if (PlatformInfo.isAndroid) {
                Text(stringResource(Res.string.append_http_proxy_sum))
            }
        },
    )

    HttpProxyBypassPreference(appendHttpProxyValue, needReload)

    val allowAccessValue by DataStore.allowAccess.collectAsStateWithLifecycle()
    SwitchPreference(
        value = allowAccessValue,
        onValueChange = {
            DataStore.allowAccess.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.allow_access)) },
        icon = {
            MaskedIcon(Res.drawable.nat, color = IconMaskColors.IconCoral)
        },
        summary = { Text(stringResource(Res.string.allow_access_sum)) },
    )

    val inboundUsernameValue by DataStore.inboundUsername.collectAsStateWithLifecycle()
    TextFieldPreference(
        value = inboundUsernameValue,
        onValueChange = {
            DataStore.inboundUsername.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.inbound_username)) },
        textToValue = { it },
        icon = {
            MaskedIcon(Res.drawable.person, color = IconMaskColors.IconCyan)
        },
        summary = { Text(contentOrUnset(inboundUsernameValue)) },
        valueToText = { it },
    )

    val inboundPasswordValue by DataStore.inboundPassword.collectAsStateWithLifecycle()
    PasswordPreference(
        value = inboundPasswordValue,
        onValueChange = {
            DataStore.inboundPassword.setBlocking(it)
            needReload()
        },
        title = { Text(stringResource(Res.string.inbound_password)) },
    )
}
