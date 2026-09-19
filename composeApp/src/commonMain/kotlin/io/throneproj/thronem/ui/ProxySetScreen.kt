package io.throneproj.thronem.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.ProxySet
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.add_proxy_set
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.drag_indicator
import io.throneproj.thronem.resources.edit
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.playlist_add
import io.throneproj.thronem.resources.proxy_set
import io.throneproj.thronem.resources.proxy_set_delete_confirm_prompt
import io.throneproj.thronem.resources.proxy_set_status_empty
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProxySetScreen(
    modifier: Modifier = Modifier,
    viewModel: ProxySetScreenViewModel = viewModel { ProxySetScreenViewModel() },
    openProxySetSettings: (setId: Long) -> Unit,
) {
    var deleteSetConfirm by remember { mutableStateOf<Long?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowInsets = WindowInsets.safeDrawing
    val dragDropListState = rememberDragDropSwipeLazyColumnState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                title = { Text(stringResource(Res.string.proxy_set)) },
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.playlist_add),
                            contentDescription = stringResource(Res.string.add_proxy_set),
                            onClick = {
                                openProxySetSettings(0L)
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
        Row(modifier = Modifier.fillMaxSize()) {
            DragDropSwipeLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdge(dragDropListState.lazyListState),
                state = dragDropListState,
                items = uiState.sets.toImmutableList(),
                key = { it.id },
                contentType = { 0 },
                contentPadding = contentPadding,
                userScrollEnabled = true,
                onIndicesChangedViaDragAndDrop = { viewModel.submitReordered(it) },
            ) { _, item ->
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
                    dragDropEnabled = true,
                ) {
                    ProxySetCard(
                        set = item,
                        isSelected = item.id == uiState.selectedSetId,
                        onSelect = {
                            viewModel.select(item.id)
                        },
                        onEdit = {
                            openProxySetSettings(item.id)
                        },
                        onDelete = {
                            deleteSetConfirm = item.id
                        },
                        dragHandleModifier = Modifier.dragDropModifier(),
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

    deleteSetConfirm?.let { setId ->
        AlertDialog(
            onDismissRequest = { deleteSetConfirm = null },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    viewModel.delete(setId)
                    deleteSetConfirm = null
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.cancel)) {
                    deleteSetConfirm = null
                }
            },
            icon = { Icon(vectorResource(Res.drawable.delete), null) },
            title = { Text(stringResource(Res.string.proxy_set_delete_confirm_prompt)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableSwipeableItemScope<ProxySet>.ProxySetCard(
    modifier: Modifier = Modifier,
    set: ProxySet,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.drag_indicator),
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .then(dragHandleModifier),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = set.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.edit),
                        contentDescription = stringResource(Res.string.edit),
                        modifier = Modifier.size(40.dp),
                        onClick = onEdit,
                    )
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.delete),
                        contentDescription = stringResource(Res.string.delete),
                        modifier = Modifier.size(40.dp),
                        onClick = onDelete,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                ) {
                    Text(
                        text = set.displayType(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
