package io.throneproj.thronem.ui.configuration

import io.nekohasekai.libbox.Libbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import io.throneproj.thronem.GroupType
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.SheetActionRow
import io.throneproj.thronem.compose.SheetSectionTitle
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.colorForUrlTestDelay
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.focusRestoreAnchor
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.IconButton
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.rememberFocusRestoreState
import io.throneproj.thronem.compose.setPlainText
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.displayType
import io.throneproj.thronem.fmt.ValidateResult
import io.throneproj.thronem.fmt.config.ConfigBean
import io.throneproj.thronem.fmt.toUniversalLink
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.blurAddress
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.ktx.readableUrlTestError
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.action_export_clipboard
import io.throneproj.thronem.resources.action_export_file
import io.throneproj.thronem.resources.action_export_msg
import io.throneproj.thronem.resources.arrow_outward
import io.throneproj.thronem.resources.available
import io.throneproj.thronem.resources.connection_test_unreachable
import io.throneproj.thronem.resources.content_copy
import io.throneproj.thronem.resources.copy_all
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.deprecated
import io.throneproj.thronem.resources.drag_indicator
import io.throneproj.thronem.resources.edit
import io.throneproj.thronem.resources.error
import io.throneproj.thronem.resources.error_title
import io.throneproj.thronem.resources.file_export
import io.throneproj.thronem.resources.fingerprint
import io.throneproj.thronem.resources.insecure
import io.throneproj.thronem.resources.internal_link
import io.throneproj.thronem.resources.link
import io.throneproj.thronem.resources.menu_configuration
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.outbound
import io.throneproj.thronem.resources.qr_code
import io.throneproj.thronem.resources.send
import io.throneproj.thronem.resources.settings
import io.throneproj.thronem.resources.share
import io.throneproj.thronem.resources.share_qr_nfc
import io.throneproj.thronem.resources.standard
import io.throneproj.thronem.resources.traffic
import io.throneproj.thronem.resources.unavailable
import io.throneproj.thronem.resources.warning
import io.throneproj.thronem.results.LocalResultEventBus
import io.throneproj.thronem.results.ResultEffect
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.StringOrRes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private data class PendingProfileEdit(
    val resultKey: String,
    val profileId: Long,
)

@Composable
internal fun GroupHolderScreen(
    modifier: Modifier = Modifier,
    viewModel: GroupProfilesHolderViewModel,
    bottomPadding: Dp,
    showActions: Boolean = true,
    canHoldFocus: Boolean,
    onProfileSelect: ((Long) -> Unit)? = null,
    onOpenProfileEditor: ((NavRoutes.ProfileEditor) -> Unit)? = null,
    needReload: () -> Unit,
    showQR: (name: String, url: String) -> Unit,
    onCopySuccess: () -> Unit,
    showSnackbar: (message: StringOrRes) -> Unit,
    showUndoSnackbar: (count: Int, onUndo: () -> Unit) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultBus = onOpenProfileEditor?.let { LocalResultEventBus.current }
    val pendingProfileEdits = remember { mutableStateListOf<PendingProfileEdit>() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hiddenProfiles) {
        if (uiState.hiddenProfiles > 0) {
            showUndoSnackbar(uiState.hiddenProfiles) {
                viewModel.undo()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.commit()
        }
    }
    val showAddress by viewModel.alwaysShowAddress.collectAsStateWithLifecycle(false)
    val blurAddress by viewModel.blurredAddress.collectAsStateWithLifecycle(false)
    val trafficStatistics by viewModel.trafficStatistics.collectAsStateWithLifecycle(true)
    val securityAdvisory by viewModel.securityAdvisory.collectAsStateWithLifecycle(true)

    val dragDropListState = rememberDragDropSwipeLazyColumnState()
    val focusRestore = rememberFocusRestoreState()

    LaunchedEffect(uiState.scrollIndex) {
        uiState.scrollIndex?.let { index ->
            dragDropListState.lazyListState.animateScrollToItem(index)
            viewModel.consumeScrollIndex()
        }
    }

    LaunchedEffect(canHoldFocus, focusRestore.isAttached) {
        if (canHoldFocus) {
            focusRestore.restore()
        }
    }

    resultBus?.let { bus ->
        for (pending in pendingProfileEdits.toList()) {
            ResultEffect<Boolean>(
                resultEventBus = bus,
                resultKey = pending.resultKey,
            ) { updated ->
                if (updated) {
                    needReload()
                }
                pendingProfileEdits.remove(pending)
            }
        }
    }

    fun openProfileEditor(profile: ProxyEntity) {
        val resultKey = "profile-editor-${profile.id}"
        pendingProfileEdits.removeAll { it.resultKey == resultKey }
        pendingProfileEdits += PendingProfileEdit(
            resultKey = resultKey,
            profileId = profile.id,
        )
        onOpenProfileEditor?.invoke(
            NavRoutes.ProfileEditor(
                type = profile.type,
                id = profile.id,
                subscription = viewModel.group.type == GroupType.SUBSCRIPTION,
                resultKey = resultKey,
            ),
        )
    }

    var exportConfig by remember { mutableStateOf("") }
    val exportFileLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        if (file != null) lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                file.write(exportConfig.encodeToByteArray())
                withContext(Dispatchers.Main) {
                    showSnackbar(StringOrRes.Res(Res.string.action_export_msg))
                }
            } catch (e: Exception) {
                Logs.w(e)
                withContext(Dispatchers.Main) {
                    showSnackbar(StringOrRes.Direct(e.readableMessage))
                }
            }
        }
        exportConfig = ""
    }

    var showErrorAlert by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        DragDropSwipeLazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRestoreAnchor(focusRestore, canHoldFocus)
                .fadingEdge(dragDropListState.lazyListState),
            state = dragDropListState,
            items = uiState.profiles.toImmutableList(),
            key = { it.profile.id },
            contentType = { 0 },
            contentPadding = PaddingValues(
                bottom = bottomPadding,
            ),
            userScrollEnabled = true,
            onIndicesChangedViaDragAndDrop = { viewModel.submitReordered(it) },
        ) { index, item ->
            DraggableSwipeableItem(
                modifier = Modifier
                    .padding(4.dp)
                    .animateDraggableSwipeableItem(),
                colors = DraggableSwipeableItemColors.createRemembered(
                    containerBackgroundColor = Color.Transparent,
                    containerBackgroundColorWhileDragged = Color.Transparent,
                    clickIndicationColor = Color.Transparent,
                    behindSwipeContainerBackgroundColor = Color.Transparent,
                    behindSwipeIconColor = Color.Transparent,
                ),
                dragDropEnabled = uiState.canReorder,
            ) {
                ProxyCard(
                    profile = item,
                    select = onProfileSelect?.let { callback ->
                        { callback(item.profile.id) }
                    },
                    edit = {
                        openProfileEditor(item.profile)
                    },
                    delete = { viewModel.undoableRemove(item.profile.id) },
                    showQR = { url ->
                        showQR(item.profile.displayName(), url)
                    },
                    exportToFile = { name, config ->
                        exportConfig = config
                        exportFileLauncher.launch(suggestedName = name, defaultExtension = "json")
                    },
                    showErrorAlert = { showErrorAlert = it },
                    onCopySuccess = onCopySuccess,
                    showAddress = showAddress,
                    blurAddress = blurAddress,
                    trafficStatistic = trafficStatistics,
                    securityAdvice = securityAdvisory,
                    showActions = showActions,
                )
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

    if (showErrorAlert != null) AlertDialog(
        onDismissRequest = { showErrorAlert = null },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showErrorAlert = null
            }
        },
        icon = {
            Icon(vectorResource(Res.drawable.error), null)
        },
        title = { Text(stringResource(Res.string.error_title)) },
        text = { Text(showErrorAlert!!) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableSwipeableItemScope<ProfileItem>.ProxyCard(
    modifier: Modifier = Modifier,
    profile: ProfileItem,
    select: (() -> Unit)?,
    edit: () -> Unit,
    delete: () -> Unit,
    showQR: (url: String) -> Unit,
    onCopySuccess: () -> Unit,
    exportToFile: (name: String, config: String) -> Unit,
    showErrorAlert: (String) -> Unit,
    showAddress: Boolean,
    blurAddress: Boolean,
    trafficStatistic: Boolean,
    securityAdvice: Boolean,
    showActions: Boolean = true,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val entity = profile.profile
    val bean = entity.requireBean()

    val (name, address) = when {
        blurAddress && bean.name.isBlank() -> bean.displayAddress().blurAddress() to null
        blurAddress && showAddress -> bean.displayName() to bean.displayAddress().blurAddress()
        showAddress -> bean.displayName() to bean.displayAddress()
        else -> bean.displayName() to null
    }

    val hasTraffic = entity.tx + entity.rx > 0L
    val trafficText = hasTraffic.takeIf { trafficStatistic }?.let {
        stringResource(
            Res.string.traffic,
            Libbox.formatBytes(entity.tx),
            Libbox.formatBytes(entity.rx),
        )
    }

    val (statusText, statusColor) = when (entity.status) {
        in Int.MIN_VALUE..ProxyEntity.STATUS_INITIAL -> {
            trafficText.orEmpty() to MaterialTheme.colorScheme.onSurfaceVariant
        }

        ProxyEntity.STATUS_AVAILABLE -> {
            stringResource(
                Res.string.available,
                entity.ping,
            ) to colorForUrlTestDelay(entity.ping)
        }

        ProxyEntity.STATUS_UNAVAILABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.unavailable)
            text to Color.Red
        }

        ProxyEntity.STATUS_UNREACHABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.connection_test_unreachable)
            text to Color.Red
        }

        else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val showMiddleRow =
        address != null || (hasTraffic && entity.status > ProxyEntity.STATUS_INITIAL)

    var showShareSheet by remember { mutableStateOf(false) }
    var showSecurityAlert by remember { mutableStateOf(false) }
    val shareSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val validateResult = if (showActions && securityAdvice) {
        bean.isInsecure()
    } else {
        ValidateResult.Secure.Continue
    }

    // The main configuration page keeps its list items read-only; only the
    // profile picker passes a selection callback, which makes cards clickable.
    OutlinedCard(
        onClick = { select?.invoke() },
        enabled = select != null,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (profile.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.drag_indicator),
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(40.dp)
                    .padding(8.dp)
                    .dragDropModifier(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    if (showActions) {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.edit),
                            contentDescription = stringResource(Res.string.edit),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = edit,
                        )

                        val shareIcon: DrawableResource
                        val shareBackground: Color
                        val shareTint: Color
                        when (validateResult) {
                            is ValidateResult.Insecure -> {
                                shareIcon = Res.drawable.warning
                                shareBackground = Color.Red
                                shareTint = Color.White
                            }

                            is ValidateResult.Deprecated -> {
                                shareIcon = Res.drawable.warning
                                shareBackground = Color.Yellow
                                shareTint = Color.Gray
                            }

                            is ValidateResult.Secure -> {
                                shareIcon = Res.drawable.share
                                shareBackground = Color.Transparent
                                shareTint = MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }

                        Box {
                            val shareTooltipText = when (validateResult) {
                                is ValidateResult.Insecure -> stringResource(Res.string.insecure)
                                is ValidateResult.Deprecated -> stringResource(Res.string.deprecated)
                                is ValidateResult.Secure -> stringResource(Res.string.share)
                            }
                            val shareTooltipState = rememberTooltipState()

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(shareBackground, shape = CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        positioning = TooltipAnchorPosition.Below,
                                    ),
                                    tooltip = {
                                        PlainTooltip {
                                            Text(shareTooltipText)
                                        }
                                    },
                                    state = shareTooltipState,
                                ) {
                                    IconButton(
                                        onClick = {
                                            when (validateResult) {
                                                is ValidateResult.Insecure, is ValidateResult.Deprecated -> {
                                                    showSecurityAlert = true
                                                }

                                                is ValidateResult.Secure -> {
                                                    showShareSheet = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = vectorResource(shareIcon),
                                            contentDescription = shareTooltipText,
                                            tint = shareTint,
                                        )
                                    }
                                }
                            }

                            if (showShareSheet) {
                                val canNotShareOutbound = entity.type == ProxyEntity.TYPE_CHAIN ||
                                        (bean as? ConfigBean)?.type == ConfigBean.TYPE_CONFIG

                                ModalBottomSheet(
                                    onDismissRequest = { showShareSheet = false },
                                    sheetState = shareSheetState,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (entity.haveLink()) {
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.share_qr_nfc),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.qr_code),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            if (entity.haveStandardLink()) {
                                                SheetActionRow(
                                                    text = stringResource(Res.string.standard),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.send,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        showQR(entity.toStdLink())
                                                        showShareSheet = false
                                                    },
                                                )
                                            }
                                            SheetActionRow(
                                                text = stringResource(Res.string.internal_link),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.link),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    showQR(bean.toUniversalLink())
                                                    showShareSheet = false
                                                },
                                            )
                                            HorizontalDivider()
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.action_export_clipboard),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.share),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            if (entity.haveStandardLink()) {
                                                SheetActionRow(
                                                    text = stringResource(Res.string.standard),
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
                                                            clipboard.setPlainText(entity.toStdLink())
                                                            onCopySuccess()
                                                        }
                                                        showShareSheet = false
                                                    },
                                                )
                                            }
                                            SheetActionRow(
                                                text = stringResource(Res.string.internal_link),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.fingerprint),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    scope.launch {
                                                        clipboard.setPlainText(bean.toUniversalLink())
                                                        onCopySuccess()
                                                    }
                                                    showShareSheet = false
                                                },
                                            )
                                        }
                                        HorizontalDivider()
                                        SheetSectionTitle(
                                            text = stringResource(Res.string.menu_configuration),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = vectorResource(Res.drawable.settings),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            },
                                        )
                                        SheetActionRow(
                                            text = stringResource(Res.string.action_export_clipboard),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = vectorResource(Res.drawable.copy_all),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                scope.launch {
                                                    runCatching {
                                                        clipboard.setPlainText(entity.exportConfig().first)
                                                    }.onSuccess {
                                                        onCopySuccess()
                                                    }.onFailure { e ->
                                                        showErrorAlert(e.readableMessage)
                                                    }
                                                }
                                                showShareSheet = false
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
                                                scope.launch {
                                                    runCatching {
                                                        val data = entity.exportConfig()
                                                        exportToFile(data.second, data.first)
                                                    }.onFailure { e ->
                                                        showErrorAlert(e.readableMessage)
                                                    }
                                                }
                                                showShareSheet = false
                                            },
                                        )

                                        if (!canNotShareOutbound) {
                                            HorizontalDivider()
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.outbound),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.arrow_outward),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            SheetActionRow(
                                                text = stringResource(Res.string.action_export_clipboard),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.copy_all),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    scope.launch {
                                                        clipboard.setPlainText(entity.exportOutbound().first)
                                                        onCopySuccess()
                                                    }
                                                    showShareSheet = false
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
                                                    scope.launch {
                                                        val data = entity.exportOutbound()
                                                        exportToFile(data.second, data.first)
                                                    }
                                                    showShareSheet = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = delete,
                        )
                    }
                }

                if (showMiddleRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 0.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        address?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (hasTraffic && entity.status > ProxyEntity.STATUS_INITIAL) {
                            trafficText?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entity.displayType(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )

                    if (statusText.isNotEmpty()) {
                        val errorText = entity.error?.blankAsNull()
                        Text(
                            text = statusText,
                            modifier = Modifier.clickable {
                                errorText?.let(showErrorAlert)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                        )
                    }
                }
            }
        }
    }

    if (showActions && showSecurityAlert) AlertDialog(
        onDismissRequest = {
            showSecurityAlert = false
            showShareSheet = true
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning), null)
        },
        title = {
            Text(
                stringResource(
                    when (validateResult) {
                        is ValidateResult.Insecure -> Res.string.insecure
                        is ValidateResult.Deprecated -> Res.string.deprecated
                        else -> error("impossible")
                    },
                ),
            )
        },
        text = {
            val textRes = when (validateResult) {
                is ValidateResult.Insecure -> validateResult.textRes
                is ValidateResult.Deprecated -> validateResult.textRes
                else -> error("impossible")
            }
            Text(stringResource(textRes))
        },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showSecurityAlert = false
                showShareSheet = true
            }
        },
    )
}
