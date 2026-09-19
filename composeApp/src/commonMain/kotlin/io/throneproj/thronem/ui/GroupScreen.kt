package io.throneproj.thronem.ui

import io.nekohasekai.libbox.Libbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import io.throneproj.thronem.GroupType
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.QRCodeDialog
import io.throneproj.thronem.compose.SheetActionRow
import io.throneproj.thronem.compose.SheetSectionTitle
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.setPlainText
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.toUniversalLink
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.DisplayTime
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.action_export
import io.throneproj.thronem.resources.action_export_clipboard
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.action_export_file
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.clear_profiles
import io.throneproj.thronem.resources.clear_profiles_message
import io.throneproj.thronem.resources.confirm
import io.throneproj.thronem.resources.content_copy
import io.throneproj.thronem.resources.copy_success
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.drag_indicator
import io.throneproj.thronem.resources.edit
import io.throneproj.thronem.resources.file_export
import io.throneproj.thronem.resources.group_create
import io.throneproj.thronem.resources.group_status_empty
import io.throneproj.thronem.resources.group_status_empty_subscription
import io.throneproj.thronem.resources.group_status_proxies
import io.throneproj.thronem.resources.group_status_updated_empty
import io.throneproj.thronem.resources.group_update
import io.throneproj.thronem.resources.internal_link
import io.throneproj.thronem.resources.link
import io.throneproj.thronem.resources.menu
import io.throneproj.thronem.resources.menu_group
import io.throneproj.thronem.resources.mop
import io.throneproj.thronem.resources.more_vert
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.playlist_add
import io.throneproj.thronem.resources.qr_code
import io.throneproj.thronem.resources.removed
import io.throneproj.thronem.resources.share
import io.throneproj.thronem.resources.share_qr_nfc
import io.throneproj.thronem.resources.share_subscription
import io.throneproj.thronem.resources.subscription_expire
import io.throneproj.thronem.resources.subscription_last_updated
import io.throneproj.thronem.resources.subscription_traffic
import io.throneproj.thronem.resources.subscription_used
import io.throneproj.thronem.resources.undo
import io.throneproj.thronem.resources.update
import io.throneproj.thronem.resources.update_all_subscription
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Instant

@Composable
fun GroupScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    viewModel: GroupScreenViewModel = viewModel { GroupScreenViewModel() },
    onBackPress: () -> Unit,
    openGroupSettings: (Long) -> Unit,
) {
    val snackbar = LocalSnackbarEmitter.current
    DisposableEffect(Unit) {
        onDispose {
            viewModel.commit()
        }
    }

    var showUpdateAll by remember { mutableStateOf(false) }
    var qrDialogData by remember { mutableStateOf<Pair<String, String>?>(null) } // url:name
    var clearGroupConfirm by remember { mutableStateOf<Long?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.hiddenGroups) {
        if (uiState.hiddenGroups > 0) {
            snackbar.show(
                StringOrRes.PluralsRes(
                    Res.plurals.removed,
                    uiState.hiddenGroups,
                    uiState.hiddenGroups,
                ),
                StringOrRes.Res(Res.string.undo),
            ) { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undo()
                }
            }
        }
    }

    var groupToExport by remember { mutableStateOf<Long?>(null) }
    val exportProfiles = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        if (file != null && groupToExport != null) {
            viewModel.exportToFile(
                group = groupToExport!!,
                writeContent = { content ->
                    file.write(content.encodeToByteArray())
                },
                showSnackbar = snackbar::show,
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowInsets = WindowInsets.safeDrawing
    val dragDropListState = rememberDragDropSwipeLazyColumnState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                title = { Text(stringResource(Res.string.menu_group)) },
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.arrow_back),
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackPress,
                    )
                },
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.update),
                            contentDescription = stringResource(Res.string.update_all_subscription),
                            onClick = { showUpdateAll = true },
                        )
                    }
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.playlist_add),
                            contentDescription = stringResource(Res.string.group_create),
                            onClick = {
                                openGroupSettings(0L)
                            },
                        )
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding.withNavigation()
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            DragDropSwipeLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdge(dragDropListState.lazyListState),
                state = dragDropListState,
                items = uiState.groups.toImmutableList(),
                key = { it.group.id },
                contentType = { 0 },
                userScrollEnabled = true,
                contentPadding = contentPadding,
                onIndicesChangedViaDragAndDrop = { viewModel.submitReorder(it) },
            ) { _, groupState ->
                val removable = !groupState.group.ungrouped && !groupState.isUpdating

                DraggableSwipeableItem(
                    modifier = Modifier.animateDraggableSwipeableItem(),
                    colors = DraggableSwipeableItemColors.createRemembered(
                        containerBackgroundColor = Color.Transparent,
                        containerBackgroundColorWhileDragged = Color.Transparent,
                    ),
                    allowedSwipeDirections = if (removable) {
                        AllowedSwipeDirections.All
                    } else {
                        AllowedSwipeDirections.None
                    },
                    onSwipeDismiss = { viewModel.undoableRemove(groupState.group.id) },
                ) {
                    GroupCard(
                        mainViewModel = mainViewModel,
                        state = groupState,
                        openGroupSettings = openGroupSettings,
                        snackbar = { message ->
                            snackbar.show(StringOrRes.Direct(message))
                        },
                        showQRDialog = { url, name ->
                            qrDialogData = url to name
                        },
                        showClearGroupDialog = {
                            clearGroupConfirm = groupState.group.id
                        },
                        exportToFile = {
                            groupToExport = groupState.group.id
                            exportProfiles.launch(
                                suggestedName = "profiles_${groupState.group.displayName()}",
                                defaultExtension = "txt",
                            )
                        },
                    )
                }
            }

            BoxedVerticalScrollbar(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = dragDropListState.lazyListState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }

    if (showUpdateAll) AlertDialog(
        onDismissRequest = { showUpdateAll = false },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                mainViewModel.updateAllSubscriptionGroups()
                showUpdateAll = false
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.cancel)) {
                showUpdateAll = false
            }
        },
        icon = { Icon(vectorResource(Res.drawable.update), null) },
        title = { Text(stringResource(Res.string.confirm)) },
        text = { Text(stringResource(Res.string.update_all_subscription)) },
    )

    qrDialogData?.let { (url, name) ->
        QRCodeDialog(
            url = url,
            name = name,
            onDismiss = { qrDialogData = null },
            showSnackbar = { message ->
                snackbar.show(StringOrRes.Direct(message))
            },
        )
    }

    clearGroupConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { clearGroupConfirm = null },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    viewModel.clearGroup(id)
                    clearGroupConfirm = null
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.cancel)) {
                    clearGroupConfirm = null
                }
            },
            icon = { Icon(vectorResource(Res.drawable.mop), null) },
            title = { Text(stringResource(Res.string.confirm)) },
            text = { Text(stringResource(Res.string.clear_profiles_message)) },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableSwipeableItemScope<GroupItemUiState>.GroupCard(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    state: GroupItemUiState,
    openGroupSettings: (Long) -> Unit,
    snackbar: suspend (message: String) -> Unit,
    showQRDialog: (url: String, name: String) -> Unit,
    showClearGroupDialog: () -> Unit,
    exportToFile: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val group = state.group

    var showOptionsSheet by remember { mutableStateOf(false) }
    val optionsSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isUpdating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (state.updateProgress != null) {
                LinearProgressIndicator(
                    progress = { state.updateProgress.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                            .padding(start = 0.dp, end = 4.dp, top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = group.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )

                                val subscription = group.subscription
                                subscription?.username.blankAsNull()?.let { username ->
                                    Text(
                                        text = username,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Row {
                            if (!group.ungrouped) {
                                SimpleIconButton(
                                    imageVector = vectorResource(Res.drawable.edit),
                                    contentDescription = stringResource(Res.string.edit),
                                    onClick = {
                                        openGroupSettings(group.id)
                                    },
                                )
                            }

                            Box {
                                SimpleIconButton(
                                    imageVector = vectorResource(Res.drawable.more_vert),
                                    contentDescription = stringResource(Res.string.menu),
                                    onClick = { showOptionsSheet = true },
                                )

                                if (showOptionsSheet) {
                                    ModalBottomSheet(
                                        onDismissRequest = { showOptionsSheet = false },
                                        sheetState = optionsSheetState,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            val subscriptionLink =
                                                group.subscription?.link?.blankAsNull()

                                            subscriptionLink?.let { link ->
                                                SheetSectionTitle(
                                                    text = stringResource(Res.string.share_subscription),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.share,
                                                            ),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    },
                                                )
                                                SheetActionRow(
                                                    text = stringResource(Res.string.action_export_clipboard),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.content_copy,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        scope.launch {
                                                            clipboard.setPlainText(link)
                                                            snackbar(
                                                                resolveRepository().getString(
                                                                    Res.string.copy_success,
                                                                ),
                                                            )
                                                        }
                                                        showOptionsSheet = false
                                                    },
                                                )
                                                SheetActionRow(
                                                    text = stringResource(Res.string.share_qr_nfc),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.qr_code,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        showQRDialog(link, group.displayName())
                                                        showOptionsSheet = false
                                                    },
                                                )
                                            }
                                            if (group.subscription != null) {
                                                HorizontalDivider()
                                                SheetSectionTitle(
                                                    text = stringResource(Res.string.internal_link),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.link,
                                                            ),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    },
                                                )
                                                SheetActionRow(
                                                    text = stringResource(Res.string.action_export_clipboard),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.content_copy,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        scope.launch {
                                                            clipboard.setPlainText(group.toUniversalLink())
                                                            snackbar(
                                                                resolveRepository().getString(
                                                                    Res.string.copy_success,
                                                                ),
                                                            )
                                                        }
                                                        showOptionsSheet = false
                                                    },
                                                )
                                                SheetActionRow(
                                                    text = stringResource(Res.string.share_qr_nfc),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.qr_code,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        showQRDialog(
                                                            group.toUniversalLink(),
                                                            group.displayName(),
                                                        )
                                                        showOptionsSheet = false
                                                    },
                                                )
                                            }
                                            HorizontalDivider()
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.action_export),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.file_export),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            SheetActionRow(
                                                text = stringResource(Res.string.action_export_clipboard),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.content_copy),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    scope.launch {
                                                        val links = withContext(Dispatchers.IO) {
                                                            ThroneDatabase.proxyDao
                                                                .getByGroup(group.id)
                                                                .first()
                                                        }.joinToString("\n") {
                                                            it.toStdLink()
                                                        }
                                                        clipboard.setPlainText(links)
                                                        snackbar(resolveRepository().getString(Res.string.copy_success))
                                                    }
                                                    showOptionsSheet = false
                                                },
                                            )
                                            SheetActionRow(
                                                text = stringResource(Res.string.action_export_file),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.file_export),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    exportToFile()
                                                    showOptionsSheet = false
                                                },
                                            )
                                            HorizontalDivider()
                                            SheetActionRow(
                                                text = stringResource(Res.string.clear_profiles),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.delete),
                                                        contentDescription = null,
                                                    )
                                                },
                                                textColor = MaterialTheme.colorScheme.error,
                                                iconTint = MaterialTheme.colorScheme.error,
                                                onClick = {
                                                    showOptionsSheet = false
                                                    showClearGroupDialog()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val subscription = group.subscription
                            if (subscription != null &&
                                (subscription.bytesUsed > 0L || subscription.bytesRemaining > 0)
                            ) {
                                Text(
                                    text = if (subscription.bytesRemaining > 0L) {
                                        stringResource(
                                            Res.string.subscription_traffic,
                                            Libbox.formatBytes(subscription.bytesUsed),
                                            Libbox.formatBytes(subscription.bytesRemaining),
                                        )
                                    } else {
                                        stringResource(
                                            Res.string.subscription_used,
                                            Libbox.formatBytes(subscription.bytesUsed),
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (subscription.expiryDate > 0) {
                                    Text(
                                        text = stringResource(
                                            Res.string.subscription_expire,
                                            DisplayTime.date(Instant.fromEpochSeconds(subscription.expiryDate)),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (state.group.type == GroupType.SUBSCRIPTION &&
                                state.group.subscription!!.lastUpdated > 0
                            ) {
                                Text(
                                    text = stringResource(
                                        Res.string.subscription_last_updated,
                                        DisplayTime.dateTime(
                                            Instant.fromEpochSeconds(
                                                state.group.subscription!!.lastUpdated.toLong(),
                                            ),
                                        ),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Text(
                                text = when (state.group.type) {
                                    GroupType.BASIC -> {
                                        if (state.counts == 0L) {
                                            stringResource(Res.string.group_status_empty)
                                        } else {
                                            pluralStringResource(
                                                Res.plurals.group_status_proxies,
                                                state.counts.toInt(),
                                                state.counts,
                                            )
                                        }
                                    }

                                    GroupType.SUBSCRIPTION -> {
                                        if (state.counts == 0L) {
                                            if (state.group.subscription!!.lastUpdated > 0) {
                                                stringResource(Res.string.group_status_updated_empty)
                                            } else {
                                                stringResource(Res.string.group_status_empty_subscription)
                                            }
                                        } else {
                                            pluralStringResource(
                                                Res.plurals.group_status_proxies,
                                                state.counts.toInt(),
                                                state.counts,
                                            )
                                        }
                                    }

                                    else -> stringResource(Res.string.group_status_empty)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (group.type == GroupType.SUBSCRIPTION) {
                            TextButton(
                                onClick = { mainViewModel.updateSubscriptionGroup(group) },
                                modifier = Modifier.padding(end = 8.dp),
                                enabled = !state.isUpdating,
                            ) {
                                Text(
                                    text = stringResource(Res.string.group_update),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
