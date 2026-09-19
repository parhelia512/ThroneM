package io.throneproj.thronem.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.bg.BackendState
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.connect
import io.throneproj.thronem.resources.description
import io.throneproj.thronem.resources.directions
import io.throneproj.thronem.resources.disconnect
import io.throneproj.thronem.resources.menu_configuration
import io.throneproj.thronem.resources.menu_dashboard
import io.throneproj.thronem.resources.menu_route
import io.throneproj.thronem.resources.play_arrow
import io.throneproj.thronem.resources.settings
import io.throneproj.thronem.resources.stop
import io.throneproj.thronem.resources.transform
import io.throneproj.thronem.resources.vpn_permission_denied
import io.throneproj.thronem.ui.LocalSnackbarEmitter
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.StringOrRes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Floating connection bar (ported from KunBox MainBottomBar): four mutually
 * exclusive navigation buttons (Dashboard/Configuration/Route/Settings) plus a
 * large circular connect/stop button on the right.
 */
@Composable
fun MainBottomBar(
    selectedRoute: NavRoutes?,
    onSelectRoute: (NavRoutes) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val serviceStatus by BackendState.status.collectAsStateWithLifecycle()
    val snackbarEmitter = LocalSnackbarEmitter.current
    val vpnConnector = rememberVpnServiceLauncher {
        snackbarEmitter.show(StringOrRes.Res(Res.string.vpn_permission_denied))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(barColor)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainBottomBarItem(
                label = Res.string.menu_dashboard,
                icon = Res.drawable.transform,
                route = NavRoutes.Dashboard,
                selectedRoute = selectedRoute,
                onSelectRoute = onSelectRoute,
            )
            Spacer(Modifier.width(8.dp))
            MainBottomBarItem(
                label = Res.string.menu_configuration,
                icon = Res.drawable.description,
                route = NavRoutes.Configuration,
                selectedRoute = selectedRoute,
                onSelectRoute = onSelectRoute,
            )
            Spacer(Modifier.width(8.dp))
            MainBottomBarItem(
                label = Res.string.menu_route,
                icon = Res.drawable.directions,
                route = NavRoutes.Route,
                selectedRoute = selectedRoute,
                onSelectRoute = onSelectRoute,
            )
            Spacer(Modifier.width(8.dp))
            MainBottomBarItem(
                label = Res.string.settings,
                icon = Res.drawable.settings,
                route = NavRoutes.Settings,
                selectedRoute = selectedRoute,
                onSelectRoute = onSelectRoute,
            )
            Spacer(Modifier.weight(1f))

            // Start/stop toggle button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (serviceStatus.state.canStop) {
                            resolveRepository().stopService()
                        } else {
                            vpnConnector()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(
                        if (serviceStatus.state.canStop) Res.drawable.stop else Res.drawable.play_arrow,
                    ),
                    contentDescription = stringResource(
                        if (serviceStatus.state.canStop) Res.string.disconnect else Res.string.connect,
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RowScope.MainBottomBarItem(
    label: StringResource,
    icon: DrawableResource,
    route: NavRoutes,
    selectedRoute: NavRoutes?,
    onSelectRoute: (NavRoutes) -> Unit,
) {
    val selected = selectedRoute != null && selectedRoute::class == route::class
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(enabled = !selected) { onSelectRoute(route) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = stringResource(label),
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
    }
}
