package io.throneproj.thronem.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.throneproj.thronem.compose.BackHandler
import io.throneproj.thronem.compose.BoxedVerticalScrollbar
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.LinkOrContentTextField
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.ValidatedTextField
import io.throneproj.thronem.compose.fadingEdge
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.compose.withNavigation
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.assets_settings
import io.throneproj.thronem.resources.auto_update_off
import io.throneproj.thronem.resources.auto_update_on
import io.throneproj.thronem.resources.cancel
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.delete
import io.throneproj.thronem.resources.delete_confirm_prompt
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.emoji_symbols
import io.throneproj.thronem.resources.error_title
import io.throneproj.thronem.resources.link
import io.throneproj.thronem.resources.no
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.route_asset_auto_update_delay
import io.throneproj.thronem.resources.route_asset_name
import io.throneproj.thronem.resources.timer
import io.throneproj.thronem.resources.unsaved_changes_prompt
import io.throneproj.thronem.resources.url
import io.throneproj.thronem.resources.warning
import io.throneproj.thronem.resources.warning_amber
import io.throneproj.thronem.results.LocalResultEventBus
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Serializable
sealed interface AssetEditResult {

    @Serializable
    data object Saved : AssetEditResult

    @Serializable
    data class Created(
        val assetName: String,
    ) : AssetEditResult

    @Serializable
    data class ShouldUpdate(
        val assetName: String,
    ) : AssetEditResult

    @Serializable
    data class Deleted(
        val assetName: String,
    ) : AssetEditResult

    @Serializable
    data object Canceled : AssetEditResult

}

@Composable
internal fun AssetEditScreen(
    assetName: String,
    resultKey: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AssetEditViewModel = viewModel { AssetEditViewModel(assetName) },
) {
    val resultBus = LocalResultEventBus.current

    val isDirty by viewModel.isDirty.collectAsState()
    var showBackAlert by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        if (isDirty) {
            showBackAlert = true
        } else {
            resultBus.sendResult<AssetEditResult>(resultKey, AssetEditResult.Canceled)
            onBack()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var illegalNameMessage by remember { mutableStateOf<StringOrRes?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun saveAndExit() {
        viewModel.save()
        val currentName = viewModel.uiState.value.name
        val result = when {
            viewModel.isNew -> AssetEditResult.Created(currentName)
            viewModel.shouldUpdateFromInternet -> AssetEditResult.ShouldUpdate(currentName)
            else -> AssetEditResult.Saved
        }
        resultBus.sendResult(resultKey, result)
        onBack()
    }

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
                    ) {
                        if (isDirty) {
                            showBackAlert = true
                        } else {
                            resultBus.sendResult<AssetEditResult>(
                                resultKey,
                                AssetEditResult.Canceled,
                            )
                            onBack()
                        }
                    }
                },
                title = { Text(stringResource(Res.string.assets_settings)) },
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                        ) {
                            val editingAssetName = viewModel.editingName
                            if (editingAssetName.isEmpty()) {
                                resultBus.sendResult<AssetEditResult>(
                                    resultKey,
                                    AssetEditResult.Canceled,
                                )
                                onBack()
                            } else {
                                showDeleteConfirm = true
                            }
                        }
                    }
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.done),
                            contentDescription = stringResource(Res.string.apply),
                        ) {
                            saveAndExit()
                        }
                    }
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        ProvidePreferenceLocals {
            val contentPadding = innerPadding.withNavigation()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .fadingEdge(
                            scrollableState = listState,
                            fadeStart = true,
                            fadeEnd = true,
                        ),
                    contentPadding = contentPadding,
                ) {
                    preferenceGroup(key = "settings") {
                        TextFieldPreference(
                            value = uiState.name,
                            onValueChange = { viewModel.setName(it) },
                            title = { Text(stringResource(Res.string.route_asset_name)) },
                            textToValue = { it },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.emoji_symbols,
                                    color = IconMaskColors.IconCyan,
                                )
                            },
                            summary = { Text(contentOrUnset(uiState.name)) },
                            valueToText = { it },
                            textField = { value, onValueChange, onOk ->
                                ValidatedTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    onOk = onOk,
                                    validator = { name ->
                                        viewModel.validate(name)?.let { getStringOrRes(it) }
                                    },
                                )
                            },
                        )
                        TextFieldPreference(
                            value = uiState.link,
                            onValueChange = { viewModel.setLink(it) },
                            title = { Text(stringResource(Res.string.url)) },
                            textToValue = { it },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.link,
                                    color = IconMaskColors.IconLightBlue,
                                )
                            },
                            summary = { Text(contentOrUnset(uiState.link)) },
                            valueToText = { it },
                            textField = { value, onValueChange, onOk ->
                                LinkOrContentTextField(value, onValueChange, onOk)
                            },
                        )
                        TextFieldPreference(
                            value = uiState.autoUpdateDelay,
                            onValueChange = { viewModel.setAutoUpdateDelay(it) },
                            title = {
                                Text(stringResource(Res.string.route_asset_auto_update_delay))
                            },
                            textToValue = { it.toIntOrNull() ?: 0 },
                            icon = {
                                MaskedIcon(
                                    Res.drawable.timer,
                                    color = IconMaskColors.IconLightOrange,
                                )
                            },
                            summary = {
                                Text(
                                    if (uiState.autoUpdateDelay > 0) {
                                        stringResource(Res.string.auto_update_on, uiState.autoUpdateDelay)
                                    } else {
                                        stringResource(Res.string.auto_update_off)
                                    },
                                )
                            },
                            valueToText = { it.toString() },
                            textField = { value, onValueChange, onOk ->
                                UIntegerTextField(value, onValueChange, onOk)
                            },
                        )
                    }

                    item("bottom_padding") {
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
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

    if (showBackAlert) {
        AlertDialog(
            onDismissRequest = { showBackAlert = false },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    coroutineScope.launch {
                        viewModel.validate(viewModel.uiState.value.name)?.let {
                            illegalNameMessage = it
                            showBackAlert = false
                            return@launch
                        }
                        saveAndExit()
                    }
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.no)) {
                    resultBus.sendResult<AssetEditResult>(resultKey, AssetEditResult.Canceled)
                    onBack()
                }
            },
            icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
            title = { Text(stringResource(Res.string.unsaved_changes_prompt)) },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    resultBus.sendResult<AssetEditResult>(
                        resultKey,
                        AssetEditResult.Deleted(viewModel.editingName),
                    )
                    onBack()
                }
            },
            dismissButton = {
                TextButton(stringResource(Res.string.cancel)) {
                    resultBus.sendResult<AssetEditResult>(resultKey, AssetEditResult.Canceled)
                    onBack()
                }
            },
            icon = { Icon(vectorResource(Res.drawable.warning), null) },
            title = { Text(stringResource(Res.string.delete_confirm_prompt)) },
        )
    }

    illegalNameMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { illegalNameMessage = null },
            confirmButton = {
                TextButton(stringResource(Res.string.ok)) {
                    illegalNameMessage = null
                }
            },
            icon = { Icon(vectorResource(Res.drawable.warning_amber), null) },
            title = { Text(stringResource(Res.string.error_title)) },
            text = { Text(stringOrRes(message)) },
        )
    }
}
