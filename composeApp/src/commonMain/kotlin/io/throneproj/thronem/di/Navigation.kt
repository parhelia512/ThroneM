@file:OptIn(KoinExperimentalAPI::class)

package io.throneproj.thronem.di

import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.bg.BackendState
import io.throneproj.thronem.results.LocalResultEventBus
import io.throneproj.thronem.ui.AboutScreen
import io.throneproj.thronem.ui.AssetEditScreen
import io.throneproj.thronem.ui.AssetsScreen
import io.throneproj.thronem.ui.GroupScreen
import io.throneproj.thronem.ui.GroupSettingsScreen
import io.throneproj.thronem.ui.LibrariesScreen
import io.throneproj.thronem.ui.ProxySetScreen
import io.throneproj.thronem.ui.ProxySetSettingsScreen
import io.throneproj.thronem.ui.LogcatScreen
import io.throneproj.thronem.ui.Navigator
import io.throneproj.thronem.ui.MainScreenScope
import io.throneproj.thronem.ui.MainViewModel
import io.throneproj.thronem.ui.SnackbarEmitter
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.ProfilePickerController
import io.throneproj.thronem.ui.RouteScreen
import io.throneproj.thronem.ui.RouteSettingsScreen
import io.throneproj.thronem.ui.configuration.ConfigurationScreen
import io.throneproj.thronem.ui.warp.WarpWizardScreen
import io.throneproj.thronem.ui.dashboard.DashboardScreen
import io.throneproj.thronem.ui.jsoneditor.ConfigEditScreen
import io.throneproj.thronem.ui.profile.ProfileEditorScreen
import io.throneproj.thronem.ui.profile.SIP003EditorScreen
import io.throneproj.thronem.ui.settings.SettingsPageScreen
import io.throneproj.thronem.ui.settings.SettingsScreen
import io.throneproj.thronem.ui.tools.BackupScreen
import io.throneproj.thronem.ui.tools.DebugScreen
import io.throneproj.thronem.ui.tools.NetworkQualityScreen
import io.throneproj.thronem.ui.tools.NetworkScreen
import io.throneproj.thronem.ui.tools.StunScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.dsl.onClose

internal val commonNavigationModule = module {
    scope<MainScreenScope> {
        scopedOf(::MainViewModel) onClose { it?.close() }
        scoped { (backStack: MutableList<NavKey>) ->
            Navigator(backStack)
        }
        scopedOf(::ProfilePickerController)
        scopedOf(::SnackbarEmitter)

        navigation<NavRoutes.Configuration> { _ ->
            val viewModel = get<MainViewModel>()
            val navigator = get<Navigator>()
            ConfigurationScreen(
                mainViewModel = viewModel,
                onOpenGroups = { navigator.navigateTo(NavRoutes.Groups) },
                onOpenGroupSettings = { groupId ->
                    navigator.navigateTo(NavRoutes.GroupSettings(groupId = groupId))
                },
                onOpenProfileEditor = navigator::navigateTo,
                onOpenWarpWizard = { navigator.navigateTo(NavRoutes.WarpWizard) },
            )
        }

        navigation<NavRoutes.Groups> { _ ->
            val viewModel = get<MainViewModel>()
            val navigator = get<Navigator>()
            GroupScreen(
                mainViewModel = viewModel,
                onBackPress = { navigator.popBackStack() },
                openGroupSettings = { groupId ->
                    navigator.navigateTo(NavRoutes.GroupSettings(groupId = groupId))
                },
            )
        }

        navigation<NavRoutes.ProxySetSettings> { route ->
            val navigator = get<Navigator>()
            val profilePickerController = get<ProfilePickerController>()
            ProxySetSettingsScreen(
                setId = route.setId,
                onBackPress = { navigator.popBackStack() },
                onOpenProfileSelect = profilePickerController::open,
            )
        }

        navigation<NavRoutes.Route> { _ ->
            val navigator = get<Navigator>()
            RouteScreen(
                openRouteSettings = { routeId ->
                    navigator.navigateTo(NavRoutes.RouteSettings(routeId = routeId))
                },
                openAssets = {
                    navigator.navigateTo(NavRoutes.Assets)
                },
            )
        }

        navigation<NavRoutes.Settings> { _ ->
            val navigator = get<Navigator>()
            SettingsScreen(
                openSettingsPage = { kind ->
                    navigator.navigateTo(NavRoutes.SettingsPage(kind))
                },
                openTool = navigator::navigateTo,
                openAbout = { navigator.navigateTo(NavRoutes.About) },
                openMainPage = { navigator.navigateTo(it) },
            )
        }

        navigation<NavRoutes.SettingsPage> { route ->
            val navigator = get<Navigator>()
            SettingsPageScreen(
                kind = route.kind,
                onBackPress = { navigator.popBackStack() },
                openAppManager = { navigator.navigateTo(NavRoutes.AppManager) },
            )
        }

        navigation<NavRoutes.Log> { _ ->
            val navigator = get<Navigator>()
            LogcatScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.Dashboard> { _ ->
            val navigator = get<Navigator>()
            // The Dashboard entry shares its bottom-bar icon with the proxy sets
            // picker: when the service is down we surface ProxySetScreen so the
            // user can pick a set, and switch to the live Dashboard once connected.
            val serviceStatus by BackendState.status.collectAsStateWithLifecycle()
            if (serviceStatus.state.connected) {
                DashboardScreen(
                    openConnectController = get(),
                    openVPNController = get(),
                    openRouteSettings = { initialState ->
                        navigator.navigateTo(
                            NavRoutes.RouteSettings(
                                routeId = -1L,
                                useDraft = true,
                                initialState = initialState,
                            ),
                        )
                    },
                )
            } else {
                ProxySetScreen(
                    openProxySetSettings = { setId ->
                        navigator.navigateTo(NavRoutes.ProxySetSettings(setId = setId))
                    },
                )
            }
        }

        navigation<NavRoutes.ProfileEditor> { route ->
            val navigator = get<Navigator>()
            val profilePickerController = get<ProfilePickerController>()
            val resultBus = LocalResultEventBus.current
            ProfileEditorScreen(
                type = route.type,
                profileId = route.id,
                isSubscription = route.subscription,
                onOpenProfileSelect = profilePickerController::open,
                onOpenConfigEditor = navigator::navigateTo,
                onOpenSIP003Editor = navigator::navigateTo,
                onResult = { updated ->
                    resultBus.sendResult(route.resultKey, updated)
                    navigator.popBackStack()
                },
            )
        }

        navigation<NavRoutes.WarpWizard> { _ ->
            val navigator = get<Navigator>()
            WarpWizardScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.GroupSettings> { route ->
            val navigator = get<Navigator>()
            GroupSettingsScreen(
                groupId = route.groupId,
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.RouteSettings> { route ->
            val navigator = get<Navigator>()
            val profilePickerController = get<ProfilePickerController>()
            RouteSettingsScreen(
                routeId = route.routeId,
                initialState = route.initialState.takeIf { route.useDraft },
                onBackPress = { navigator.popBackStack() },
                onSaved = { navigator.popBackStack() },
                onOpenProfileSelect = profilePickerController::open,
                onOpenAppList = navigator::navigateTo,
                onOpenConfigEditor = navigator::navigateTo,
            )
        }

        navigation<NavRoutes.ConfigEditor> { route ->
            val navigator = get<Navigator>()
            ConfigEditScreen(
                initialText = route.initialText,
                resultKey = route.resultKey,
                schema = route.schema,
                onBack = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.SIP003Editor> { route ->
            val navigator = get<Navigator>()
            SIP003EditorScreen(
                pluginName = route.pluginName,
                initialOpts = route.initialOpts,
                resultKey = route.resultKey,
                onBack = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.Assets> { _ ->
            val navigator = get<Navigator>()
            AssetsScreen(
                onBackPress = { navigator.popBackStack() },
                onOpenAssetEditor = navigator::navigateTo,
            )
        }

        navigation<NavRoutes.AssetEdit> { route ->
            val navigator = get<Navigator>()
            AssetEditScreen(
                assetName = route.assetName,
                resultKey = route.resultKey,
                onBack = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.ToolsPage.Network> { _ ->
            val navigator = get<Navigator>()
            NetworkScreen(
                onBackPress = { navigator.popBackStack() },
                onOpenTool = navigator::navigateTo,
            )
        }

        navigation<NavRoutes.ToolsPage.Backup> { _ ->
            val navigator = get<Navigator>()
            BackupScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.ToolsPage.Debug> { _ ->
            val navigator = get<Navigator>()
            DebugScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.ToolsPage.Stun> { _ ->
            val navigator = get<Navigator>()
            StunScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.ToolsPage.NetworkQuality> { _ ->
            val navigator = get<Navigator>()
            NetworkQualityScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.About> { _ ->
            val navigator = get<Navigator>()
            AboutScreen(
                onBackPress = { navigator.popBackStack() },
                onNavigateToLibraries = {
                    navigator.navigateTo(NavRoutes.Libraries)
                },
            )
        }

        navigation<NavRoutes.Libraries> { _ ->
            val navigator = get<Navigator>()
            LibrariesScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }
    }
}
