@file:OptIn(KoinExperimentalAPI::class, KoinDelicateAPI::class)

package io.throneproj.thronem.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.throneproj.thronem.bg.BackendState
import io.throneproj.thronem.bg.ServiceAlert
import io.throneproj.thronem.compose.BackHandler
import io.throneproj.thronem.compose.MainBottomBar
import io.throneproj.thronem.compose.ScrollableDialog
import io.throneproj.thronem.compose.SwipeableSnackbarHost
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.NavigationSuite
import io.throneproj.thronem.compose.material3.NavigationSuiteItem
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.navigationBarsAlwaysInsets
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.ktx.restartApplication
import io.throneproj.thronem.ktx.runOnDefaultDispatcher
import io.throneproj.thronem.permission.AppPermission
import io.throneproj.thronem.permission.LocalPermissionPlatform
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.access_local_network_denied
import io.throneproj.thronem.resources.action_download
import io.throneproj.thronem.resources.bug_report
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.description
import io.throneproj.thronem.resources.directions
import io.throneproj.thronem.resources.error
import io.throneproj.thronem.resources.have_a_nice_day
import io.throneproj.thronem.resources.location_permission_description
import io.throneproj.thronem.resources.location_permission_title
import io.throneproj.thronem.resources.menu
import io.throneproj.thronem.resources.menu_configuration
import io.throneproj.thronem.resources.menu_dashboard
import io.throneproj.thronem.resources.menu_log
import io.throneproj.thronem.resources.menu_route
import io.throneproj.thronem.resources.no_thanks
import io.throneproj.thronem.resources.auth_later_hint
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.permission_denied
import io.throneproj.thronem.resources.query_package_denied
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.settings
import io.throneproj.thronem.resources.transform
import io.throneproj.thronem.resources.warning_amber
import io.throneproj.thronem.results.LocalResultEventBus
import io.throneproj.thronem.results.ResultEventBus
import io.throneproj.thronem.ui.configuration.ProfileSelectSheet
import io.throneproj.thronem.ui.openconnect.OpenConnectAuthController
import io.throneproj.thronem.ui.openconnect.OpenConnectAuthDialog
import io.throneproj.thronem.ui.openvpn.OpenVPNAuthController
import io.throneproj.thronem.ui.openvpn.OpenVPNAuthDialog
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.navigation3.EntryProvider
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    moveToBackground: () -> Unit,
    initialProcessText: String? = null,
) {
    val koin = getKoin()
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides rememberViewModelStoreOwner(),
    ) {
        val mainScreenHolder = viewModel { MainScreenScopeHolder(koin) }
        val mainScreenScope = mainScreenHolder.scope
        UnboundKoinScope(mainScreenScope) {
            val entryProvider = koinEntryProvider<NavKey>(scope = mainScreenScope)
            MainScreenContent(
                modifier = modifier,
                viewModel = mainScreenScope.get<MainViewModel>(),
                moveToBackground = moveToBackground,
                initialProcessText = initialProcessText,
                koinScope = mainScreenScope,
                entryProvider = entryProvider,
                backStack = mainScreenHolder.backStack,
            )
        }
    }
}

@Composable
private fun MainScreenContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    moveToBackground: () -> Unit,
    initialProcessText: String?,
    koinScope: Scope,
    entryProvider: EntryProvider<NavKey>,
    backStack: MutableList<NavKey>,
) {
    val permission = LocalPermissionPlatform.current
    val uriHandler = LocalUriHandler.current

    val resultBus = remember { ResultEventBus() }
    val navigator = remember(koinScope, backStack) {
        koinScope.get<Navigator> {
            parametersOf(backStack)
        }
    }
    val selectedTopLevelRoute = navigator.selectedTopLevelRoute
    val isAtStartDestination = navigator.isAtStartDestination
    val profilePickerController = remember(koinScope) {
        koinScope.get<ProfilePickerController>()
    }
    val snackbarEmitter = remember(koinScope) { koinScope.get<SnackbarEmitter>() }
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarEmitterEffect(snackbarEmitter, snackbarHostState)
    var mainDialog by remember { mutableStateOf<MainAlertDialogEvent?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.dialogEvent.collect { mainDialog = it }
    }

    /**
     * Check query packages permission for rogue vendors.
     * If we don't query for `com.android.permission.GET_INSTALLED_APPS` permission,
     * only when we query all packages in foreground will pop the permission window for query permission.
     * @see <a href="https://www.taf.org.cn/upload/AssociationStandard/TTAF%20108-2022%20%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E5%BA%94%E7%94%A8%E8%BD%AF%E4%BB%B6%E5%88%97%E8%A1%A8%E6%9D%83%E9%99%90%E5%AE%9E%E6%96%BD%E6%8C%87%E5%8D%97.pdf">Mobile App Package Listing Permission Implementation Guide</a>
     */
    var showQueryPackageDeniedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (permission.canRequestPermission(AppPermission.QueryInstalledApps) &&
            !permission.hasPermission(AppPermission.QueryInstalledApps)
        ) {
            permission.requestPermission(AppPermission.QueryInstalledApps) { granted ->
                if (granted) runOnDefaultDispatcher {
                    resolveRepository().stopService()
                    delay(500.milliseconds)
                    ThroneDatabase.instance.close()
                    restartApplication()
                } else {
                    showQueryPackageDeniedDialog = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPostNotification =
            permission.hasPermission(AppPermission.PostNotifications)
        if (!hasPostNotification) {
            permission.requestPermission(AppPermission.PostNotifications)
        }
    }

    var showLocalNetworkDeniedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (permission.canRequestPermission(AppPermission.LocalNetwork) &&
            !permission.hasPermission(AppPermission.LocalNetwork)
        ) {
            permission.requestPermission(AppPermission.LocalNetwork) { granted ->
                if (!granted) showLocalNetworkDeniedDialog = true
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            !isAtStartDestination -> {
                val popped = navigator.popBackStack()
                if (!popped) {
                    navigator.navigateToTopLevelRoute(navigator.startDestination)
                }
            }

            else -> moveToBackground()
        }
    }

    LaunchedEffect(initialProcessText) {
        if (!initialProcessText.isNullOrBlank()) {
            viewModel.parseProxy(initialProcessText)
        }
    }

    var showServiceAlert by remember { mutableStateOf<ServiceAlert?>(null) }

    LaunchedEffect(Unit) {
        BackendState.alerts.collect { alert ->
            when (alert) {
                is ServiceAlert.Common -> {
                    if (alert.message.isNotBlank()) {
                        snackbarEmitter.show(StringOrRes.Direct(alert.message))
                    }
                }
                is ServiceAlert.NeedWifiPermission -> {
                    showServiceAlert = alert
                }
            }
        }
    }

    val topLevelDestinations = remember {
        persistentListOf(
            TopLevelDestination(
                Res.string.menu_configuration,
                Res.drawable.description,
                NavRoutes.Configuration,
            ),
            TopLevelDestination(
                Res.string.menu_dashboard,
                Res.drawable.transform,
                NavRoutes.Dashboard,
            ),
            TopLevelDestination(Res.string.menu_route, Res.drawable.directions, NavRoutes.Route),
            TopLevelDestination(Res.string.menu_log, Res.drawable.bug_report, NavRoutes.Log),
            TopLevelDestination(Res.string.settings, Res.drawable.settings, NavRoutes.Settings),
        )
    }
    val navigationItems = topLevelDestinations.map { destination ->
        val selected = selectedTopLevelRoute.matchesRoute(destination.route)
        NavigationSuiteItem(
            label = destination.label,
            icon = destination.icon,
            selected = selected,
            onClick = {
                if (!selected) {
                    navigator.navigateToTopLevelRoute(destination.route)
                }
            },
        )
    }.toPersistentList()

    // The main bottom bar replaces the old navigation bar + FAB on the phone
    // shell; it hosts the top-level destinations it switches between
    // (Log stays reachable via Settings; TV keeps its drawer).
    val showMainBottomBar = navigator.isCurrentTopLevel && (
        selectedTopLevelRoute.matchesRoute(NavRoutes.Configuration) ||
            selectedTopLevelRoute.matchesRoute(NavRoutes.Dashboard) ||
            selectedTopLevelRoute.matchesRoute(NavRoutes.Route) ||
            selectedTopLevelRoute.matchesRoute(NavRoutes.Settings)
        )

    CompositionLocalProvider(
        LocalResultEventBus provides resultBus,
        LocalSnackbarEmitter provides snackbarEmitter,
    ) {
        NavigationSuite(
            items = navigationItems,
            showNavigation = showMainBottomBar,
            snackbarHost = {
                SwipeableSnackbarHost(
                    hostState = snackbarHostState,
                    windowInsets = if (navigator.isCurrentTopLevel) {
                        WindowInsets(0, 0, 0, 0)
                    } else {
                        navigationBarsAlwaysInsets()
                    },
                )
            },
            bottomBar = {
                MainBottomBar(
                    selectedRoute = selectedTopLevelRoute,
                    onSelectRoute = navigator::navigateToTopLevelRoute,
                )
            },
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = navigator::popBackStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )

            profilePickerController.session?.let { session ->
                ProfileSelectSheet(
                    preSelected = session.preSelected,
                    onDismiss = profilePickerController::dismiss,
                    onSelected = profilePickerController::select,
                )
            }

            val openConnectController = koinInject<OpenConnectAuthController>()
            val pendingOpenConnectAuth by openConnectController.pendingDialogAuth
                .collectAsStateWithLifecycle()
            pendingOpenConnectAuth?.let { pending ->
                OpenConnectAuthDialog(
                    pending = pending,
                    controller = openConnectController,
                    showError = { message ->
                        snackbarEmitter.show(StringOrRes.Direct(message))
                    },
                    onDismissed = {
                        snackbarEmitter.show(
                            StringOrRes.Res(Res.string.auth_later_hint),
                        )
                    },
                )
            }

            val openVPNController = koinInject<OpenVPNAuthController>()
            val pendingOpenVPNAuth by openVPNController.pendingDialogAuth
                .collectAsStateWithLifecycle()
            pendingOpenVPNAuth?.let { pending ->
                OpenVPNAuthDialog(
                    pending = pending,
                    controller = openVPNController,
                    showError = { message ->
                        snackbarEmitter.show(StringOrRes.Direct(message))
                    },
                    onDismissed = {
                        snackbarEmitter.show(
                            StringOrRes.Res(Res.string.auth_later_hint),
                        )
                    },
                )
            }
        }
    }

    if (showQueryPackageDeniedDialog) AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                permission.openPermissionSettings()
                showQueryPackageDeniedDialog = false
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.no_thanks)) {
                showQueryPackageDeniedDialog = false
                snackbarEmitter.show(StringOrRes.Res(Res.string.have_a_nice_day))
            }
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning_amber), null)
        },
        title = { Text(stringResource(Res.string.permission_denied)) },
        text = { Text(stringResource(Res.string.query_package_denied)) },
    )

    if (showLocalNetworkDeniedDialog) AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showLocalNetworkDeniedDialog = false
                permission.requestPermission(AppPermission.LocalNetwork) { granted ->
                    if (!granted) showLocalNetworkDeniedDialog = true
                }
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.no_thanks)) {
                showLocalNetworkDeniedDialog = false
            }
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning_amber), null)
        },
        title = { Text(stringResource(Res.string.permission_denied)) },
        text = { Text(stringResource(Res.string.access_local_network_denied)) },
    )

    if (showServiceAlert != null) {
        when (val alert = showServiceAlert!!) {
            is ServiceAlert.NeedWifiPermission -> {
                AlertDialog(
                    onDismissRequest = { showServiceAlert = null },
                    confirmButton = {
                        TextButton(stringResource(Res.string.ok)) {
                            showServiceAlert = null
                            permission.requestPermission(AppPermission.WifiInfo)
                        }
                    },
                    dismissButton = {
                        TextButton(stringResource(Res.string.no_thanks)) {
                            showServiceAlert = null
                        }
                    },
                    icon = { Icon(vectorResource(Res.drawable.warning_amber), null) },
                    title = { Text(stringResource(Res.string.location_permission_title)) },
                    text = { Text(stringResource(Res.string.location_permission_description)) },
                )
            }

            is ServiceAlert.Common -> Unit
        }
    }

    mainDialog?.let { dialog ->
        MainViewModelAlertDialog(dialog) { mainDialog = null }
    }
}

@Immutable
private data class TopLevelDestination(
    val label: StringResource,
    val icon: DrawableResource,
    val route: NavRoutes,
)

private fun NavRoutes?.matchesRoute(
    route: NavRoutes,
): Boolean {
    val current = this ?: return false
    return current::class == route::class
}

@Composable
private fun MainViewModelAlertDialog(
    dialog: MainAlertDialogEvent,
    onConsumed: () -> Unit,
) {
    ScrollableDialog(
        onDismissRequest = {
            dialog.onDismiss?.invoke()
            onConsumed()
        },
        confirmButton = {
            TextButton(stringOrRes(dialog.confirmButton.label)) {
                dialog.confirmButton.onClick()
                onConsumed()
            }
        },
        dismissButton = dialog.dismissButton?.let { button ->
            {
                TextButton(stringOrRes(button.label)) {
                    button.onClick()
                    onConsumed()
                }
            }
        },
        icon = {
            Icon(
                vectorResource(
                    if (dialog.dismissButton != null) {
                        Res.drawable.question_mark
                    } else {
                        Res.drawable.error
                    },
                ),
                null,
            )
        },
        title = { Text(stringOrRes(dialog.title)) },
        text = { Text(stringOrRes(dialog.message)) },
    )
}
