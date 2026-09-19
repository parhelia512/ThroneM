@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.throneproj.thronem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleSearchInputField
import io.throneproj.thronem.compose.CapsuleSearchTopBar
import io.throneproj.thronem.compose.SheetActionRow
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.ansiEscape
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.RadioButton
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.setPlainText
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.action_copy
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.arrow_back
import io.throneproj.thronem.resources.back
import io.throneproj.thronem.resources.clear_logcat
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.connecting
import io.throneproj.thronem.resources.copy_all
import io.throneproj.thronem.resources.delete_sweep
import io.throneproj.thronem.resources.keyboard_arrow_down
import io.throneproj.thronem.resources.logcat
import io.throneproj.thronem.resources.more
import io.throneproj.thronem.resources.more_vert
import io.throneproj.thronem.resources.pause
import io.throneproj.thronem.resources.play_arrow
import io.throneproj.thronem.resources.resume
import io.throneproj.thronem.resources.scroll_to_bottom
import io.throneproj.thronem.resources.search
import io.throneproj.thronem.resources.search_go
import io.throneproj.thronem.resources.share
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun LogcatScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
    viewModel: LogcatScreenViewModel = viewModel {
        LogcatScreenViewModel()
    },
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val snackbar = LocalSnackbarEmitter.current
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    val isAtBottom by remember {
        derivedStateOf {
            !listState.canScrollForward
        }
    }

    var expandMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.initialize(true)
    }
    val queryLowerCase by remember {
        derivedStateOf { uiState.searchQuery?.lowercase() }
    }
    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    wasScrolling = true
                } else if (wasScrolling) {
                    autoScroll = isAtBottom
                    wasScrolling = false
                }
            }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { uiState.logs.size }
            .collect { size ->
                if (size == 0) {
                    autoScroll = true
                    return@collect
                }
                if (!uiState.pause && autoScroll) {
                    listState.scrollToItem(size - 1)
                }
            }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbar.show(StringOrRes.Direct(message))
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowInsets = WindowInsets.safeDrawing
    val searchBarState = rememberSearchBarState()
    val searchTextFieldState = viewModel.searchTextFieldState
    val searchInputField: @Composable () -> Unit = {
        CapsuleSearchInputField(
            textFieldState = searchTextFieldState,
            searchBarState = searchBarState,
            onSearch = { focusManager.clearFocus() },
            placeholder = { Text(stringResource(Res.string.search_go)) },
            leadingIcon = {
                Icon(vectorResource(Res.drawable.search), null)
            },
            trailingIcon = if (searchBarState.currentValue == SearchBarValue.Expanded) {
                {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.cancel),
                        onClick = {
                            viewModel.clearSearchQuery()
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                    )
                }
            } else {
                null
            },
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                CapsuleSearchTopBar(
                    inputField = searchInputField,
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
                                imageVector = vectorResource(
                                    if (uiState.pause) {
                                        Res.drawable.play_arrow
                                    } else {
                                        Res.drawable.pause
                                    },
                                ),
                                contentDescription = stringResource(
                                    if (uiState.pause) Res.string.resume else Res.string.pause,
                                ),
                                onClick = viewModel::togglePause,
                            )
                        }
                        CapsuleActionButton {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.keyboard_arrow_down),
                                contentDescription = stringResource(Res.string.scroll_to_bottom),
                                onClick = {
                                    if (uiState.logs.isNotEmpty()) scope.launch {
                                        listState.animateScrollToItem(uiState.logs.lastIndex)
                                    }
                                },
                            )
                        }
                        CapsuleActionButton {
                            Box {
                                SimpleIconButton(
                                    imageVector = vectorResource(Res.drawable.more_vert),
                                    contentDescription = stringResource(Res.string.more),
                                    onClick = { expandMenu = true },
                                )
                                DropdownMenuPopup(
                                    expanded = expandMenu,
                                    onDismissRequest = { expandMenu = false },
                                ) {
                                    DropdownMenuGroup(
                                        shapes = MenuDefaults.groupShape(0, 2),
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.clear_logcat)) },
                                            onClick = viewModel::clearLog,
                                            leadingIcon = {
                                                Icon(vectorResource(Res.drawable.delete_sweep), null)
                                            },
                                            colors = MenuDefaults.itemColors().copy(
                                                leadingIconColor = MaterialTheme.colorScheme.error,
                                            ),
                                            shape = MenuDefaults.itemShape(0, 2).shape,
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.logcat)) },
                                            onClick = {
                                                expandMenu = false
                                                showBottomSheet = true
                                            },
                                            leadingIcon = {
                                                Icon(vectorResource(Res.drawable.share), null)
                                            },
                                            shape = MenuDefaults.itemShape(1, 2).shape,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))

                                    DropdownMenuGroup(
                                        shapes = MenuDefaults.groupShape(1, 2),
                                    ) {
                                        val levels = logLevels
                                        for ((index, level) in levels.withIndex()) {
                                            DropdownMenuItem(
                                                text = { Text(level.name) },
                                                onClick = {
                                                    viewModel.setLogLevel(level)
                                                    expandMenu = false
                                                },
                                                trailingIcon = {
                                                    RadioButton(
                                                        selected = uiState.logLevel == level,
                                                        onClick = null,
                                                    )
                                                },
                                                shape = MenuDefaults.itemShape(index, levels.size).shape,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    windowInsets = windowInsets.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding(),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (uiState.connecting) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularWavyProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(Res.string.connecting),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .fadingEdge(listState),
                                state = listState,
                                contentPadding = contentPadding,
                            ) {
                                itemsIndexed(
                                    items = uiState.logs,
                                    key = { index, _ -> index },
                                    contentType = { _, _ -> 0 },
                                ) { _, logLine ->
                                    LogCard(
                                        logLine = logLine.message,
                                        highlightQuery = queryLowerCase,
                                    )
                                }
                            }
                        }
                    }
                }

                BoxedVerticalScrollbar(
                    modifier = Modifier.fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState),
                    style = defaultMaterialScrollbarStyle().copy(
                        thickness = 12.dp,
                    ),
                )
            }
        }
    }

    if (showBottomSheet) ModalBottomSheet(
        onDismissRequest = { showBottomSheet = false },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SheetActionRow(
                text = stringResource(Res.string.action_copy),
                leadingIcon = {
                    Icon(vectorResource(Res.drawable.copy_all), null)
                },
                onClick = {
                    scope.launch {
                        clipboard.setPlainText(viewModel.buildExportLog().content)
                    }
                },
            )
            ShareActionRow(
                scope = scope,
                buildLog = viewModel::buildExportLog,
            ) { e ->
                snackbar.show(StringOrRes.Direct(e.readableMessage))
            }
        }
    }
}

@Composable
private fun LogCard(
    modifier: Modifier = Modifier,
    logLine: String,
    highlightQuery: String? = null,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = logLine.ansiEscape(highlightQuery),
            modifier = Modifier.padding(12.dp),
        )
    }
}
