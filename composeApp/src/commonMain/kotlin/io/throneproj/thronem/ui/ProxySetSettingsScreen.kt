package io.throneproj.thronem.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import io.throneproj.thronem.compose.BackHandler
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.DropDownSelector
import io.throneproj.thronem.compose.DurationTextField
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.ScrollableDialog
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.TooltipIconButton
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.displayType
import io.throneproj.thronem.fmt.internal.ProxySetBean
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.action_balancer
import io.throneproj.thronem.resources.action_selector
import io.throneproj.thronem.resources.action_urltest
import io.throneproj.thronem.resources.add_group
import io.throneproj.thronem.resources.add_profile
import io.throneproj.thronem.resources.add_source
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.cast_connected
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.connection_test_url
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.drag_indicator
import io.throneproj.thronem.resources.edit
import io.throneproj.thronem.resources.emoji_emotions
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.filter_regex
import io.throneproj.thronem.resources.flip_camera_android
import io.throneproj.thronem.resources.group_settings
import io.throneproj.thronem.resources.idle_timeout
import io.throneproj.thronem.resources.interrupt_exist_connections
import io.throneproj.thronem.resources.management
import io.throneproj.thronem.resources.menu_group
import io.throneproj.thronem.resources.not_set
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.photo_camera
import io.throneproj.thronem.resources.profile_name
import io.throneproj.thronem.resources.proxy_set_delete_confirm_prompt
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.rotate_interval
import io.throneproj.thronem.resources.stop
import io.throneproj.thronem.resources.unsaved_changes_prompt
import io.throneproj.thronem.resources.urltest_interval
import io.throneproj.thronem.resources.urltest_tolerance
import io.throneproj.thronem.resources.widgets
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private data class GroupProviderEdit(val index: Int, val item: ProviderUiItem.Group?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProxySetSettingsScreen(
    setId: Long,
    onBackPress: () -> Unit,
    onOpenProfileSelect: OpenProfilePicker,
    modifier: Modifier = Modifier,
    viewModel: ProxySetSettingsViewModel = viewModel { ProxySetSettingsViewModel(setId) },
) {
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showBackAlert by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showGenericAlert by remember { mutableStateOf<ProxySetSettingsUiEvent.Alert?>(null) }
    var groupEdit by remember { mutableStateOf<GroupProviderEdit?>(null) }

    BackHandler(enabled = isDirty) {
        showBackAlert = true
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProxySetSettingsUiEvent.Alert -> showGenericAlert = event
            }
        }
    }

    fun saveAndExit() {
        viewModel.save()
        onBackPress()
    }

    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.close),
                        onClick = {
                            if (isDirty) {
                                showBackAlert = true
                            } else {
                                onBackPress()
                            }
                        },
                    )
                },
                title = { Text(stringResource(Res.string.group_settings)) },
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                            onClick = {
                                if (viewModel.isNew) {
                                    onBackPress()
                                } else {
                                    showDeleteAlert = true
                                }
                            },
                        )
                    }
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.done),
                            contentDescription = stringResource(Res.string.apply),
                            onClick = {
                                saveAndExit()
                            },
                        )
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        ProvidePreferenceLocals {
            val uiState by viewModel.uiState.collectAsState()
            val contentPadding = innerPadding.withNavigation()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .fadingEdge(
                            scrollableState = listState,
                            fadeStart = true,
                            fadeEnd = true,
                        ),
                    contentPadding = contentPadding,
                ) {
                    proxySetSettings(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAddProfile = {
                            viewModel.replacing = -1
                            onOpenProfileSelect(null) { id ->
                                viewModel.replacing = -1
                                viewModel.onSelectProfile(id)
                            }
                        },
                        onReplaceProfile = { index, selectedProfileId ->
                            viewModel.replacing = index
                            onOpenProfileSelect(selectedProfileId.takeIf { it > 0 }) { id ->
                                viewModel.onSelectProfile(id)
                            }
                        },
                        onAddGroup = { groupEdit = GroupProviderEdit(index = -1, item = null) },
                        onEditGroup = { index, item -> groupEdit = GroupProviderEdit(index, item) },
                    )
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    groupEdit?.let { edit ->
        GroupProviderDialog(
            groups = uiState.groups,
            item = edit.item,
            onDismissRequest = { groupEdit = null },
            onConfirm = { groupID, filterNotRegex ->
                groupEdit = null
                if (edit.index < 0) {
                    viewModel.addGroupProvider(groupID, filterNotRegex)
                } else {
                    viewModel.setGroupProvider(edit.index, groupID, filterNotRegex)
                }
            },
        )
    }

    if (showBackAlert) {
        AlertDialog(
            onDismissRequest = { showBackAlert = false },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    saveAndExit()
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.cancel)) {
                    onBackPress()
                }
            },
            icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
            title = { Text(stringResource(Res.string.unsaved_changes_prompt)) },
        )
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    showDeleteAlert = false
                    viewModel.delete()
                    onBackPress()
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.cancel)) {
                    showDeleteAlert = false
                }
            },
            icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
            title = { Text(stringResource(Res.string.proxy_set_delete_confirm_prompt)) },
        )
    }

    showGenericAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { showGenericAlert = null },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    showGenericAlert = null
                }
            },
            icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
            title = { Text(stringOrRes(alert.title)) },
            text = { Text(stringOrRes(alert.message)) },
        )
    }
}

private fun LazyListScope.proxySetSettings(
    uiState: ProxySetSettingsUiState,
    viewModel: ProxySetSettingsViewModel,
    onAddProfile: () -> Unit,
    onReplaceProfile: (index: Int, profileId: Long) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (index: Int, item: ProviderUiItem.Group) -> Unit,
) {
    preferenceGroup {
        TextFieldPreference(
            value = uiState.name,
            onValueChange = { viewModel.setName(it) },
            title = { Text(stringResource(Res.string.profile_name)) },
            textToValue = { it },
            icon = { MaskedIcon(Res.drawable.emoji_symbols) },
            summary = { Text(contentOrUnset(uiState.name)) },
            valueToText = { it },
        )
        fun managementName(management: Int) =
            when (management) {
                ProxySetBean.MANAGEMENT_SELECTOR -> Res.string.action_selector
                ProxySetBean.MANAGEMENT_URLTEST -> Res.string.action_urltest
                ProxySetBean.MANAGEMENT_BALANCER -> Res.string.action_balancer
                else -> error("impossible")
            }
        ListPreference(
            value = uiState.management,
            onValueChange = { viewModel.setManagement(it) },
            values = ProxySetBean.MANAGEMENTS,
            title = { Text(stringResource(Res.string.management)) },
            icon = { MaskedIcon(Res.drawable.widgets, IconMaskColors.IconLavender) },
            summary = { Text(stringResource(managementName(uiState.management))) },
            type = ListPreferenceType.DROPDOWN_MENU,
            valueToText = { AnnotatedString(stringResource(managementName(it))) },
        )
        if (uiState.management != ProxySetBean.MANAGEMENT_BALANCER) {
            SwitchPreference(
                value = uiState.interruptExistConnections,
                onValueChange = { viewModel.setInterruptExistConnections(it) },
                title = { Text(stringResource(Res.string.interrupt_exist_connections)) },
                icon = { MaskedIcon(Res.drawable.stop, IconMaskColors.IconCoral) },
            )
        }
        if (uiState.management == ProxySetBean.MANAGEMENT_BALANCER) {
            IntervalPreference(
                value = uiState.interval,
                onValueChange = { viewModel.setInterval(it) },
                title = Res.string.rotate_interval,
            )
        }
        if (uiState.management == ProxySetBean.MANAGEMENT_URLTEST) {
            TextFieldPreference(
                value = uiState.testURL,
                onValueChange = { viewModel.setTestURL(it) },
                title = { Text(stringResource(Res.string.connection_test_url)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        resource = Res.drawable.cast_connected,
                        color = IconMaskColors.IconLightOrange,
                        shape = IconMaskShapes.route(),
                    )
                },
                summary = { Text(contentOrUnset(uiState.testURL)) },
                valueToText = { it },
            )
            IntervalPreference(
                value = uiState.interval,
                onValueChange = { viewModel.setInterval(it) },
                title = Res.string.urltest_interval,
            )
            TextFieldPreference(
                value = uiState.testIdleTimeout,
                onValueChange = { viewModel.setTestIdleTimeout(it) },
                title = { Text(stringResource(Res.string.idle_timeout)) },
                textToValue = { it },
                icon = { MaskedIcon(Res.drawable.photo_camera, IconMaskColors.IconWarmGray) },
                summary = { Text(contentOrUnset(uiState.testIdleTimeout)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    DurationTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = uiState.testTolerance,
                onValueChange = { viewModel.setTestTolerance(it) },
                title = { Text(stringResource(Res.string.urltest_tolerance)) },
                textToValue = { it.toIntOrNull() ?: 50 },
                icon = { MaskedIcon(Res.drawable.emoji_emotions, IconMaskColors.IconLightGreen) },
                summary = { Text(uiState.testTolerance.toString()) },
                valueToText = { it.toString() },
                textField = { value, onValueChange, onOk ->
                    UIntegerTextField(value, onValueChange, onOk)
                },
            )
        }
    }

    item("add_source") {
        AddSourceCard(onAddProfile = onAddProfile, onAddGroup = onAddGroup)
    }

    item("list") {
        val density = LocalDensity.current
        val windowInfo = LocalWindowInfo.current
        val maxHeight =
            with(density) { windowInfo.containerSize.height.toDp() }.takeIf { it > 0.dp }
                ?: 480.dp
        DragDropSwipeLazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
            items = uiState.providers,
            key = { it.key },
            contentType = { 0 },
            userScrollEnabled = false,
            onIndicesChangedViaDragAndDrop = { viewModel.submitReorder(it) },
        ) { i, provider ->
            DraggableSwipeableItem(
                modifier = Modifier.animateDraggableSwipeableItem(),
                colors =
                    DraggableSwipeableItemColors.createRemembered(
                        containerBackgroundColor = Color.Transparent,
                        containerBackgroundColorWhileDragged = Color.Transparent,
                    ),
                allowedSwipeDirections = AllowedSwipeDirections.None,
            ) {
                val dragHandleModifier = Modifier.dragDropModifier()
                when (provider) {
                    is ProviderUiItem.Profile -> {
                        val profile = provider.entity
                        ProxySetSourceCard(
                            title = profile.displayName(),
                            summary = profile.displayType(),
                            onEdit = {
                                onReplaceProfile(i, provider.entity.id)
                            },
                            onRemove = {
                                viewModel.remove(i)
                            },
                            dragHandleModifier = dragHandleModifier,
                        )
                    }

                    is ProviderUiItem.Group -> ProxySetGroupCard(
                        group = uiState.groups[provider.groupID],
                        filterNotRegex = provider.filterNotRegex,
                        onEdit = { onEditGroup(i, provider) },
                        onRemove = { viewModel.remove(i) },
                        dragHandleModifier = dragHandleModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalPreference(
    value: String,
    onValueChange: (String) -> Unit,
    title: StringResource,
) {
    TextFieldPreference(
        value = value,
        onValueChange = onValueChange,
        title = { Text(stringResource(title)) },
        textToValue = { it },
        icon = {
            MaskedIcon(
                resource = Res.drawable.flip_camera_android,
                color = IconMaskColors.IconLightBlue,
            )
        },
        summary = { Text(contentOrUnset(value)) },
        valueToText = { it },
        textField = { fieldValue, onFieldValueChange, onOk ->
            DurationTextField(fieldValue, onFieldValueChange, onOk)
        },
    )
}

@Composable
private fun AddSourceCard(onAddProfile: () -> Unit, onAddGroup: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Text(
                text = stringResource(Res.string.add_source),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            DropdownMenuItem(
                selected = false,
                text = { Text(stringResource(Res.string.add_profile)) },
                onClick = {
                    expanded = false
                    onAddProfile()
                },
                shapes = MenuDefaults.itemShape(0, 2),
            )
            DropdownMenuItem(
                selected = false,
                text = { Text(stringResource(Res.string.add_group)) },
                onClick = {
                    expanded = false
                    onAddGroup()
                },
                shapes = MenuDefaults.itemShape(1, 2),
            )
        }
    }
}

@Composable
private fun GroupProviderDialog(
    groups: Map<Long, ProxyGroup>,
    item: ProviderUiItem.Group?,
    onDismissRequest: () -> Unit,
    onConfirm: (groupID: Long, filterNotRegex: String) -> Unit,
) {
    val groupIDs = groups.keys.toList()
    var groupID by remember {
        mutableLongStateOf(item?.groupID ?: groupIDs.firstOrNull() ?: -1L)
    }
    var filterNotRegex by remember { mutableStateOf(item?.filterNotRegex.orEmpty()) }
    val notSet = stringResource(Res.string.not_set)

    ScrollableDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.menu_group)) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(groupID, filterNotRegex) },
                enabled = groupID > 0L,
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DropDownSelector(
                label = { Text(stringResource(Res.string.menu_group)) },
                value = groupID,
                values = groupIDs,
                onValueChange = { groupID = it },
                displayValue = { groups[it]?.displayName() ?: notSet },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = filterNotRegex,
                onValueChange = { filterNotRegex = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.filter_regex)) },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ProxySetGroupCard(
    group: ProxyGroup?,
    filterNotRegex: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    ProxySetSourceCard(
        title = group?.displayName() ?: stringResource(Res.string.not_set),
        summary = stringResource(Res.string.filter_regex) + ": " + contentOrUnset(filterNotRegex),
        onEdit = onEdit,
        onRemove = onRemove,
        dragHandleModifier = dragHandleModifier,
        modifier = modifier,
    )
}

@Composable
private fun ProxySetSourceCard(
    title: String,
    summary: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = vectorResource(Res.drawable.drag_indicator),
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(40.dp)
                    .padding(8.dp)
                    .then(dragHandleModifier),
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TooltipIconButton(
                        onClick = onEdit,
                        icon = vectorResource(Res.drawable.edit),
                        contentDescription = stringResource(Res.string.edit),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    )
                    TooltipIconButton(
                        onClick = onRemove,
                        icon = vectorResource(Res.drawable.delete),
                        contentDescription = stringResource(Res.string.delete),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = summary,
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
