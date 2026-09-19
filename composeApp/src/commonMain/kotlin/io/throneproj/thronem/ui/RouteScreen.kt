@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.throneproj.thronem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.IconButton
import io.throneproj.thronem.compose.material3.Switch
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.RuleEntity
import io.throneproj.thronem.database.RuleEntity.Companion.OUTBOUND_BLOCK
import io.throneproj.thronem.database.RuleEntity.Companion.OUTBOUND_BRIDGE
import io.throneproj.thronem.database.RuleEntity.Companion.OUTBOUND_DIRECT
import io.throneproj.thronem.database.RuleEntity.Companion.OUTBOUND_PROXY
import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.platform.PlatformInfo
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.add_road
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.apps_message
import io.throneproj.thronem.resources.cag_dns
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.clear_profiles_message
import io.throneproj.thronem.resources.confirm
import io.throneproj.thronem.resources.dns_only
import io.throneproj.thronem.resources.drag_indicator
import io.throneproj.thronem.resources.edit
import io.throneproj.thronem.resources.error_title
import io.throneproj.thronem.resources.layers
import io.throneproj.thronem.resources.menu_route
import io.throneproj.thronem.resources.more
import io.throneproj.thronem.resources.more_vert
import io.throneproj.thronem.resources.need_reload
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.process
import io.throneproj.thronem.resources.removed
import io.throneproj.thronem.resources.replay
import io.throneproj.thronem.resources.route_add
import io.throneproj.thronem.resources.route_block
import io.throneproj.thronem.resources.route_bridge
import io.throneproj.thronem.resources.route_bypass
import io.throneproj.thronem.resources.route_manage_assets
import io.throneproj.thronem.resources.route_proxy
import io.throneproj.thronem.resources.route_reset
import io.throneproj.thronem.resources.route_warn
import io.throneproj.thronem.resources.undo
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun RouteScreen(
    modifier: Modifier = Modifier,
    viewModel: RouteScreenViewModel = viewModel { RouteScreenViewModel() },
    openRouteSettings: (Long) -> Unit,
    openAssets: () -> Unit,
) {
    val snackbar = LocalSnackbarEmitter.current
    DisposableEffect(Unit) {
        onDispose {
            viewModel.commit()
        }
    }

    val dragDropListState = rememberDragDropSwipeLazyColumnState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreAction by remember { mutableStateOf(false) }
    var showResetAlert by remember { mutableStateOf(false) }
    fun needReload() {
        if (!DataStore.serviceState.started) return
        snackbar.show(
            StringOrRes.Res(Res.string.need_reload),
            StringOrRes.Res(Res.string.apply),
        ) { result ->
            if (result == SnackbarResult.ActionPerformed) {
                resolveRepository().reloadService()
            }
        }
    }

    LaunchedEffect(uiState.pendingDeleteCount) {
        if (uiState.pendingDeleteCount > 0) {
            snackbar.show(
                StringOrRes.PluralsRes(
                    Res.plurals.removed,
                    uiState.pendingDeleteCount,
                    uiState.pendingDeleteCount,
                ),
                StringOrRes.Res(Res.string.undo),
            ) { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undo()
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowInsets = WindowInsets.safeDrawing


    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                title = null,
                navigationIcon = null,
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.add_road),
                            contentDescription = stringResource(Res.string.route_add),
                            onClick = {
                                openRouteSettings(-1L)
                            },
                        )
                    }
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.layers),
                            contentDescription = stringResource(Res.string.route_manage_assets),
                            onClick = openAssets,
                        )
                    }
                    CapsuleActionButton {
                        Box {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.more_vert),
                                contentDescription = stringResource(Res.string.more),
                                onClick = { showMoreAction = true },
                            )
                            DropdownMenu(
                                expanded = showMoreAction,
                                onDismissRequest = { showMoreAction = false },
                                shape = MenuDefaults.standaloneGroupShape,
                                containerColor = MenuDefaults.groupStandardContainerColor,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.route_reset)) },
                                    onClick = {
                                        showMoreAction = false
                                        showResetAlert = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.replay),
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val listContentPadding = innerPadding.withNavigation()
        val layoutDirection = LocalLayoutDirection.current
        val listItems = remember(uiState.rules) {
            buildList(uiState.rules.size + 1) {
                add(RouteListItem.Notification)
                uiState.rules.forEach { add(RouteListItem.Rule(it)) }
            }.toImmutableList()
        }
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            DragDropSwipeLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdge(dragDropListState.lazyListState),
                state = dragDropListState,
                items = listItems,
                key = { it.key },
                contentType = { it::class },
                contentPadding = PaddingValues(
                    start = listContentPadding.calculateStartPadding(layoutDirection),
                    top = listContentPadding.calculateTopPadding(),
                    end = listContentPadding.calculateEndPadding(layoutDirection),
                    bottom = listContentPadding.calculateBottomPadding(),
                ),
                userScrollEnabled = true,
                fixedTopItemCount = RouteListItem.ROUTE_LIST_HEADER_COUNT,
                onIndicesChangedViaDragAndDrop = { ordered ->
                    viewModel.submitReorder(
                        ordered.mapNotNull { item ->
                            val rule = (item.value as? RouteListItem.Rule)?.entity
                                ?: return@mapNotNull null
                            OrderedItem(
                                value = rule,
                                initialIndex = item.initialIndex - RouteListItem.ROUTE_LIST_HEADER_COUNT,
                                newIndex = item.newIndex - RouteListItem.ROUTE_LIST_HEADER_COUNT,
                            )
                        },
                    )
                    needReload()
                },
            ) { _, item ->
                when (item) {
                    is RouteListItem.Notification -> {
                        DraggableSwipeableItem(
                            modifier = Modifier.animateDraggableSwipeableItem(),
                            colors = DraggableSwipeableItemColors.createRemembered(
                                containerBackgroundColor = Color.Transparent,
                                containerBackgroundColorWhileDragged = Color.Transparent,
                            ),
                            dragDropEnabled = false,
                            allowedSwipeDirections = AllowedSwipeDirections.None,
                        ) {
                            RouteNotificationCard()
                        }
                    }

                    is RouteListItem.Rule -> {
                        val rule = item.entity

                        DraggableSwipeableItem(
                            modifier = Modifier.animateDraggableSwipeableItem(),
                            colors = DraggableSwipeableItemColors.createRemembered(
                                containerBackgroundColor = Color.Transparent,
                                containerBackgroundColorWhileDragged = Color.Transparent,
                            ),
                            onSwipeDismiss = { viewModel.undoableRemove(rule.id) },
                        ) {
                            RuleCard(
                                rule = rule,
                                viewModel = viewModel,
                                onNeedReload = { needReload() },
                                openRouteSettings = openRouteSettings,
                            )
                        }
                    }
                }
            }

            BoxedVerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = dragDropListState.lazyListState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }

    if (showResetAlert) AlertDialog(
        onDismissRequest = { showResetAlert = false },
        title = { Text(stringResource(Res.string.confirm)) },
        text = { Text(stringResource(Res.string.clear_profiles_message)) },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showResetAlert = false
                viewModel.reset()
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.cancel)) {
                showResetAlert = false
            }
        },
    )

}

private sealed interface RouteListItem {

    companion object {
        const val ROUTE_LIST_HEADER_COUNT = 1
    }

    val key: Any

    data object Notification : RouteListItem {
        override val key: Any get() = "route_notification"
    }

    data class Rule(val entity: RuleEntity) : RouteListItem {
        override val key: Any get() = entity.id
    }

}

@Composable
private fun RouteNotificationCard(
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    ElevatedCard(
        onClick = {
            uriHandler.openUri("https://github.com/throneproj/thronem/wiki/Route")
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.route_warn),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(background = Color.Transparent),
        )
    }
}

@Composable
private fun DraggableSwipeableItemScope<RouteListItem>.RuleCard(
    modifier: Modifier = Modifier,
    rule: RuleEntity,
    viewModel: RouteScreenViewModel,
    onNeedReload: () -> Unit,
    openRouteSettings: (Long) -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.drag_indicator),
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .dragDropModifier(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rule.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        onClick = {
                            openRouteSettings(rule.id)
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.edit),
                            contentDescription = stringResource(Res.string.edit),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                ) {
                    Text(
                        text = rule.summary(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (rule.action) {
                            "", SingBoxOptions.ACTION_ROUTE -> rule.displayOutbound()
                            else -> "action: ${rule.action}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )

                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = {
                            viewModel.toggleEnabled(rule)
                            onNeedReload()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleEntity.summary(): String {
    if (dnsOnly) return stringResource(Res.string.dns_only)

    var summary = ""
    if (domains.isNotBlank()) summary += "$domains\n"
    if (ip.isNotBlank()) summary += "$ip\n"
    if (source.isNotBlank()) summary += "source: $source\n"
    if (sourcePort.isNotBlank()) summary += "sourcePort: $sourcePort\n"
    if (port.isNotBlank()) summary += "port: $port\n"
    if (network.isNotEmpty()) summary += "network: $network\n"
    if (protocol.isNotEmpty()) summary += "protocol: $protocol\n"
    if (clientType.isNotEmpty()) summary += "client: $clientType\n"
    if (packages.isNotEmpty()) {
        summary += if (PlatformInfo.isAndroid) {
            pluralStringResource(Res.plurals.apps_message, packages.size, packages.size)
        } else {
            "${stringResource(Res.string.process)}: ${packages.joinToString(", ")}"
        } + "\n"
    }
    if (packageNameRegex.isNotBlank()) summary += "packageNameRegex: $packageNameRegex\n"
    if (ssid.isNotBlank()) summary += "ssid: $ssid\n"
    if (bssid.isNotBlank()) summary += "bssid: $bssid\n"
    if (clashMode.isNotBlank()) summary += "clashMode: $clashMode\n"
    if (networkType.isNotEmpty()) summary += "networkType: $networkType\n"
    if (networkIsExpensive) summary += "networkIsExpensive\n"
    if (networkInterfaceAddress.isNotEmpty()) summary += "networkInterfaceAddress: $networkInterfaceAddress\n"

    if (overrideAddress.isNotBlank()) summary += "overrideAddress: $overrideAddress\n"
    if (overridePort > 0) summary += "overridePort: $overridePort\n"
    if (tlsFragment) {
        summary += "TLS fragment\n"
        if (tlsFragmentFallbackDelay.isNotBlank()) {
            summary += "tlsFragmentFallbackDelay: $tlsFragmentFallbackDelay\n"
        }
    }
    if (tlsRecordFragment) {
        summary += "TLS record fragment\n"
    }
    if (tlsSpoof.isNotBlank()) summary += "tlsSpoof: $tlsSpoof\n"
    if (tlsSpoofMethod.isNotBlank()) summary += "tlsSpoofMethod: $tlsSpoofMethod\n"

    if (resolveStrategy.isNotBlank()) summary += "resolveStrategy: $resolveStrategy\n"
    if (resolveDisableCache) summary += "resolveDisableCache\n"
    if (resolveRewriteTTL >= 0) summary += "resolveRewriteTTL: $resolveRewriteTTL\n"
    if (resolveClientSubnet.isNotBlank()) summary += "resolveClientSubnet: $resolveClientSubnet\n"

    if (sniffTimeout.isNotBlank()) summary += "sniffTimeout: $sniffTimeout\n"
    if (sniffers.isNotEmpty()) summary += "sniffers: $sniffers\n"

    if (customConfig.isNotBlank()) summary += stringResource(Res.string.menu_route) + "\n"
    if (customDnsConfig.isNotBlank()) summary += stringResource(Res.string.cag_dns) + "\n"

    // Even has "\n" suffix, TextView's "..." will be added and remove the last "\n".
    val lines = summary.trim().split("\n")
    return if (lines.size > 5) {
        lines.subList(0, 5).joinToString("\n", postfix = "\n...")
    } else {
        summary.trim()
    }
}

@Composable
private fun RuleEntity.displayOutbound(): String {
    return when (outbound) {
        OUTBOUND_PROXY -> stringResource(Res.string.route_proxy)
        OUTBOUND_DIRECT -> stringResource(Res.string.route_bypass)
        OUTBOUND_BLOCK -> stringResource(Res.string.route_block)
        OUTBOUND_BRIDGE -> stringResource(Res.string.route_bridge)
        else -> {
            val unknownProfile = stringResource(Res.string.error_title)
            val profileName by produceState<String?>(null, outbound) {
                value = ProfileManager.getProfile(outbound)?.displayName() ?: unknownProfile
            }
            profileName.orEmpty()
        }
    }
}
