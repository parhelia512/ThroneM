@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.throneproj.thronem.GroupType
import io.throneproj.thronem.compose.BackHandler
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.DropdownMenuSectionHeader
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.custom_config
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.delete_confirm_prompt
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.full
import io.throneproj.thronem.resources.group_status_empty
import io.throneproj.thronem.resources.more
import io.throneproj.thronem.resources.more_vert
import io.throneproj.thronem.resources.move
import io.throneproj.thronem.resources.no
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.outbound
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.unsaved_changes_prompt
import io.throneproj.thronem.resources.warning
import io.throneproj.thronem.results.ResultEffect
import io.throneproj.thronem.ui.jsoneditor.ConfigSchema
import io.throneproj.thronem.ui.NavRoutes
import io.throneproj.thronem.ui.OpenProfilePicker
import io.throneproj.thronem.ui.stringOrRes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.random.Random

@Composable
fun ProfileEditorScreen(
    type: Int,
    profileId: Long,
    isSubscription: Boolean,
    onOpenProfileSelect: OpenProfilePicker,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
    onOpenSIP003Editor: (NavRoutes.SIP003Editor) -> Unit,
    onResult: (updated: Boolean) -> Unit,
) {
    when (type) {
        ProxyEntity.TYPE_CONFIG -> ConfigSettingScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_DIRECT -> DirectSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_HTTP -> HttpSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_SOCKS -> SocksSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_SSH -> SSHSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_HYSTERIA -> HysteriaSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_ANYTLS -> AnyTLSSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_CHAIN -> ChainSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onOpenProfileSelect = onOpenProfileSelect,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_SS -> ShadowsocksSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
            onOpenSIP003Editor = onOpenSIP003Editor,
        )

        ProxyEntity.TYPE_SNELL -> SnellSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_NAIVE -> NaiveSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_JUICITY -> JuicitySettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_WG -> WireGuardSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_OPENCONNECT -> OpenConnectSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_OPENVPN -> OpenVPNSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_MASQUE -> MasqueSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_TUIC -> TuicSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_SHADOWTLS -> ShadowTLSSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_VMESS -> VMessSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_VLESS -> VLESSSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        ProxyEntity.TYPE_TROJAN -> TrojanSettingsScreen(
            profileId = profileId,
            isSubscription = isSubscription,
            onResult = onResult,
            onOpenConfigEditor = onOpenConfigEditor,
        )

        else -> error("Unsupported profile type: $type")
    }
}

@Composable
internal fun <T : AbstractBean> ProfileSettingsScreenScaffold(
    title: StringResource,
    viewModel: ProfileEditorViewModel<T>,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
    settings: LazyListScope.(
        uiState: ProfileEditorUiState,
        scrollTo: (key: String) -> Unit,
    ) -> Unit,
) {
    val isDirty by viewModel.isDirty.collectAsState()
    var showBackAlert by remember { mutableStateOf(false) }
    var showGenericAlert by remember { mutableStateOf<ProfileEditorUiEvent.Alert?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileEditorUiEvent.Alert -> showGenericAlert = event
            }
        }
    }

    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showExtendMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = isDirty) {
        showBackAlert = true
    }

    val resultNumber =
        rememberSaveable { viewModel.editingId.takeIf { it >= 0 } ?: Random.nextLong() }
    val configResultKey = remember { "config-edit-${resultNumber}" }
    val outboundConfigResultKey = remember { "outbound-config-edit-${resultNumber}" }
    ResultEffect<String?>(resultKey = configResultKey) { result ->
        if (result == null) return@ResultEffect
        viewModel.setCustomConfig(result)
    }
    ResultEffect<String?>(resultKey = outboundConfigResultKey) { result ->
        if (result == null) return@ResultEffect
        viewModel.setCustomOutbound(result)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CapsuleTopBar(
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.close),
                    ) {
                        if (isDirty) {
                            showBackAlert = true
                        } else {
                            onResult(false)
                        }
                    }
                },
                title = { Text(stringResource(title)) },
                actions = {
                    if (!viewModel.isNew) {
                        CapsuleActionButton {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.delete),
                                contentDescription = stringResource(Res.string.delete),
                                onClick = { showDeleteAlert = true },
                            )
                        }
                    }
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.done),
                            contentDescription = stringResource(Res.string.apply),
                            onClick = {
                                viewModel.save()
                                onResult(true)
                            },
                        )
                    }

                    Box {
                        CapsuleActionButton {
                            SimpleIconButton(
                                imageVector = vectorResource(Res.drawable.more_vert),
                                contentDescription = stringResource(Res.string.more),
                            ) {
                                showExtendMenu = true
                            }
                        }
                        DropdownMenuPopup(
                            expanded = showExtendMenu,
                            onDismissRequest = { showExtendMenu = false },
                        ) {
                            val showCreateShortCut =
                                platformSupportShortcut()
                                        && !viewModel.isNew
                                        && !viewModel.isSubscription
                            val showMove = !viewModel.isNew && runBlocking {
                                ThroneDatabase.groupDao.allGroups().first().filter {
                                    it.type == GroupType.BASIC
                                }.size > 1
                            }
                            val hasFirstGroup = showCreateShortCut || showMove
                            if (hasFirstGroup) {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(0, 2),
                                ) {
                                    if (showCreateShortCut) {
                                        ShortcutMenuItem(viewModel.proxyEntity) {
                                            showExtendMenu = false
                                        }
                                    }
                                    if (showMove) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.move)) },
                                            onClick = { showMoveDialog = true },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
                            }

                            DropdownMenuGroup(
                                shapes = if (hasFirstGroup) {
                                    MenuDefaults.groupShape(1, 2)
                                } else {
                                    MenuDefaults.groupShapes()
                                },
                            ) {
                                DropdownMenuSectionHeader(stringResource(Res.string.custom_config))
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.outbound)) },
                                    onClick = {
                                        showExtendMenu = false
                                        onOpenConfigEditor(
                                            NavRoutes.ConfigEditor(
                                                initialText = viewModel.uiState.value.customOutbound,
                                                resultKey = outboundConfigResultKey,
                                                schema = ConfigSchema.OUTBOUND,
                                            ),
                                        )
                                    },
                                    shape = MenuDefaults.itemShape(0, 2).shape,
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.full)) },
                                    onClick = {
                                        showExtendMenu = false
                                        onOpenConfigEditor(
                                            NavRoutes.ConfigEditor(
                                                initialText = viewModel.uiState.value.customConfig,
                                                resultKey = configResultKey,
                                            ),
                                        )
                                    },
                                    shape = MenuDefaults.itemShape(1, 2).shape,
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
        ProfileSettingsMainColumn(
            contentPadding = innerPadding.withNavigation(),
            viewModel = viewModel,
            settings = settings,
        )
    }

    if (showBackAlert) AlertDialog(
        onDismissRequest = { showBackAlert = false },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                viewModel.save()
                onResult(true)
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.no)) {
                onResult(false)
            }
        },
        icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
        title = { Text(stringResource(Res.string.unsaved_changes_prompt)) },
    )

    if (showDeleteAlert) AlertDialog(
        onDismissRequest = { showDeleteAlert = false },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showDeleteAlert = false
                viewModel.delete()
                onResult(false)
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.cancel)) {
                showDeleteAlert = false
            }
        },
        icon = { Icon(vectorResource(Res.drawable.warning), null) },
        title = { Text(stringResource(Res.string.delete_confirm_prompt)) },
    )

    if (showMoveDialog) {
        MoveProfileDialog(
            viewModel = viewModel,
            onDismiss = { showMoveDialog = false },
            onMove = {
                showMoveDialog = false
                onResult(false)
            },
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
            icon = { Icon(vectorResource(Res.drawable.warning), null) },
            title = { Text(stringOrRes(alert.title)) },
            text = { Text(stringOrRes(alert.message)) },
        )
    }
}

@Composable
private fun <T : AbstractBean> ProfileSettingsMainColumn(
    contentPadding: PaddingValues,
    viewModel: ProfileEditorViewModel<T>,
    settings: (
        scope: LazyListScope,
        uiState: ProfileEditorUiState,
        scrollTo: (key: String) -> Unit,
    ) -> Unit,
) {
    ProvidePreferenceLocals {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        var scrollToKey by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(scrollToKey) {
            listState.layoutInfo.visibleItemsInfo
                .indexOfFirst { it.key == scrollToKey }
                .takeIf { it >= 0 }?.let {
                    listState.animateScrollToItem(it)
                    scrollToKey = null
                }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdge(
                        scrollableState = listState,
                        fadeStart = true,
                        fadeEnd = true,
                    ),
                state = listState,
                contentPadding = contentPadding,
            ) {
                settings(this, uiState) { key ->
                    scrollToKey = key
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

@Composable
private fun <T : AbstractBean> MoveProfileDialog(
    viewModel: ProfileEditorViewModel<T>,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
) {
    val groups by produceState(
        initialValue = emptyList(),
        key1 = viewModel.proxyEntity.groupId,
    ) {
        value = viewModel.groupsForMove()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.move)) },
        text = {
            if (groups.isEmpty()) {
                Text(text = stringResource(Res.string.group_status_empty))
            } else {
                LazyColumn {
                    items(groups, key = { it.id }) { group ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.move(group.id)
                                        onMove()
                                    }
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = group.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(stringResource(Res.string.cancel)) {
                onDismiss()
            }
        },
    )
}
