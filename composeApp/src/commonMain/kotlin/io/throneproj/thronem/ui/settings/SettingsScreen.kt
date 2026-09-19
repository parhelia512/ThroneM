package io.throneproj.thronem.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.Preference
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.compose.plus
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.backup
import io.throneproj.thronem.resources.bug_report
import io.throneproj.thronem.resources.cag_dns
import io.throneproj.thronem.resources.cag_misc
import io.throneproj.thronem.resources.cast_connected
import io.throneproj.thronem.resources.developer_mode
import io.throneproj.thronem.resources.dns
import io.throneproj.thronem.resources.file_export
import io.throneproj.thronem.resources.flight_takeoff
import io.throneproj.thronem.resources.general_settings
import io.throneproj.thronem.resources.inbound_settings
import io.throneproj.thronem.resources.info
import io.throneproj.thronem.resources.menu_about
import io.throneproj.thronem.resources.menu_log
import io.throneproj.thronem.resources.more
import io.throneproj.thronem.resources.nat
import io.throneproj.thronem.resources.nfc
import io.throneproj.thronem.resources.ntp_category
import io.throneproj.thronem.resources.plugin
import io.throneproj.thronem.resources.protocol_settings
import io.throneproj.thronem.resources.route_options
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.settings
import io.throneproj.thronem.resources.system_daemon
import io.throneproj.thronem.resources.timelapse
import io.throneproj.thronem.resources.tools_network
import io.throneproj.thronem.resources.wifi
import io.throneproj.thronem.ui.NavRoutes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    openSettingsPage: (NavRoutes.SettingsPage.Kind) -> Unit,
    openTool: (NavRoutes) -> Unit,
    openAbout: () -> Unit,
    openMainPage: (NavRoutes) -> Unit,
) {
    val listState = rememberLazyListState()
    val isExpert by DataStore.isExpert.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        ProvidePreferenceLocals {
            val contentPadding = innerPadding.withNavigation()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .fadingEdge(listState),
                    contentPadding = contentPadding,
                ) {

                    preferenceGroup {
                        Preference(
                            title = { Text(stringResource(Res.string.general_settings)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.settings,
                                    color = IconMaskColors.IconLightBlue,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.General) },
                        )
                        if (!PlatformInfo.isAndroid) {
                            Preference(
                                title = { Text(stringResource(Res.string.system_daemon)) },
                                icon = {
                                    MaskedIcon(
                                        Res.drawable.developer_mode,
                                        color = IconMaskColors.IconLavender,
                                    )
                                },
                                onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Daemon) },
                            )
                        }
                        Preference(
                            title = { Text(stringResource(Res.string.route_options)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.router,
                                    color = IconMaskColors.IconLightGreen,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Route) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.protocol_settings)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.flight_takeoff,
                                    color = IconMaskColors.IconLightYellow,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Protocol) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.cag_dns)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.dns,
                                    color = IconMaskColors.IconCyan,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Dns) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.inbound_settings)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.nat,
                                    color = IconMaskColors.IconCoral,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Inbound) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.cag_misc)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.cast_connected,
                                    color = IconMaskColors.IconWarmGray,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Misc) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.ntp_category)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.timelapse,
                                    color = IconMaskColors.IconLightPink,
                                )
                            },
                            onClick = { openSettingsPage(NavRoutes.SettingsPage.Kind.Ntp) },
                        )
                    }

                    item { PreferenceCategory(text = { Text(stringResource(Res.string.more)) }) }
                    preferenceGroup {
                        Preference(
                            title = { Text(stringResource(Res.string.menu_log)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.bug_report,
                                    color = IconMaskColors.IconCoral,
                                )
                            },
                            onClick = { openMainPage(NavRoutes.Log) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.tools_network)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.wifi,
                                    color = IconMaskColors.IconLightBlue,
                                )
                            },
                            onClick = { openTool(NavRoutes.ToolsPage.Network) },
                        )
                        Preference(
                            title = { Text(stringResource(Res.string.backup)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.file_export,
                                    color = IconMaskColors.IconLightYellow,
                                )
                            },
                            onClick = { openTool(NavRoutes.ToolsPage.Backup) },
                        )
                        if (isExpert) {
                            Preference(
                                title = { Text("DEBUG") },
                                icon = {
                                    MaskedIcon(
                                        Res.drawable.bug_report,
                                        color = IconMaskColors.IconCoral,
                                    )
                                },
                                onClick = { openTool(NavRoutes.ToolsPage.Debug) },
                            )
                        }
                        Preference(
                            title = { Text(stringResource(Res.string.menu_about)) },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.info,
                                    color = IconMaskColors.IconLavender,
                                )
                            },
                            onClick = openAbout,
                        )
                    }
                }

                BoxedVerticalScrollbar(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState),
                    style = defaultMaterialScrollbarStyle().copy(
                        thickness = 12.dp,
                    ),
                )
            }
        }
    }
}
