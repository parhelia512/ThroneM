@file:OptIn(KoinExperimentalAPI::class)

package io.throneproj.thronem.di

import io.throneproj.thronem.ui.AppListScreen
import io.throneproj.thronem.ui.AppManagerScreen
import io.throneproj.thronem.ui.Navigator
import io.throneproj.thronem.ui.MainScreenScope
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.tools.VPNScannerScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val androidNavigationModule = module {
    scope<MainScreenScope> {
        navigation<NavRoutes.AppManager> { _ ->
            val navigator = get<Navigator>()
            AppManagerScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.AppList> { route ->
            val navigator = get<Navigator>()
            AppListScreen(
                initialPackages = route.initialPackages,
                resultKey = route.resultKey,
                onBack = { navigator.popBackStack() },
            )
        }

        navigation<NavRoutes.ToolsPage.VPNScanner> { _ ->
            val navigator = get<Navigator>()
            VPNScannerScreen(
                onBackPress = { navigator.popBackStack() },
            )
        }
    }
}
