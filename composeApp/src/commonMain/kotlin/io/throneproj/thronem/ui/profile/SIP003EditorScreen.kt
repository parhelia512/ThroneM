package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.throneproj.thronem.compose.BackHandler
import io.throneproj.thronem.compose.CapsuleActionButton
import io.throneproj.thronem.compose.CapsuleTopBar
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.IconMaskShapes
import io.throneproj.thronem.compose.ListPreference
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PreferenceCategory
import io.throneproj.thronem.compose.PreferenceType
import io.throneproj.thronem.compose.SimpleIconButton
import io.throneproj.thronem.compose.SwitchPreference
import io.throneproj.thronem.compose.TextButton
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.UIntegerTextField
import io.throneproj.thronem.compose.material3.Icon
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.apply
import io.throneproj.thronem.resources.assistant_direction
import io.throneproj.thronem.resources.certificates
import io.throneproj.thronem.resources.close
import io.throneproj.thronem.resources.done
import io.throneproj.thronem.resources.enhanced_encryption
import io.throneproj.thronem.resources.http_host
import io.throneproj.thronem.resources.http_path
import io.throneproj.thronem.resources.multiple_stop
import io.throneproj.thronem.resources.mux_number
import io.throneproj.thronem.resources.no
import io.throneproj.thronem.resources.numbers
import io.throneproj.thronem.resources.obfs_mode
import io.throneproj.thronem.resources.ok
import io.throneproj.thronem.resources.plugin
import io.throneproj.thronem.resources.question_mark
import io.throneproj.thronem.resources.router
import io.throneproj.thronem.resources.security
import io.throneproj.thronem.resources.sip003_editor
import io.throneproj.thronem.resources.sip003_pick_plugin_first
import io.throneproj.thronem.resources.tls
import io.throneproj.thronem.resources.unsaved_changes_prompt
import io.throneproj.thronem.resources.v2ray_transport
import io.throneproj.thronem.resources.vpn_key
import io.throneproj.thronem.results.LocalResultEventBus
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SIP003EditorScreen(
    pluginName: String,
    initialOpts: String,
    resultKey: String,
    onBack: () -> Unit,
) {
    val viewModel: SIP003EditorViewModel = viewModel {
        SIP003EditorViewModel(pluginName, initialOpts)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    val resultBus = LocalResultEventBus.current

    var showBackAlert by remember { mutableStateOf(false) }

    val saveAndExit: () -> Unit = {
        resultBus.sendResult<String?>(resultKey, viewModel.serialize())
        onBack()
    }
    val discardAndExit: () -> Unit = {
        resultBus.sendResult<String?>(resultKey, null)
        onBack()
    }
    val confirmBack: () -> Unit = {
        if (isDirty) {
            showBackAlert = true
        } else {
            discardAndExit()
        }
    }

    BackHandler(enabled = isDirty) {
        showBackAlert = true
    }

    val windowInsets = WindowInsets.safeDrawing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CapsuleTopBar(
                navigationIcon = {
                    SimpleIconButton(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.close),
                        onClick = confirmBack,
                    )
                },
                title = { Text(stringResource(Res.string.sip003_editor)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                actions = {
                    CapsuleActionButton {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.done),
                            contentDescription = stringResource(Res.string.apply),
                            onClick = saveAndExit,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        ProvidePreferenceLocals {
            when (pluginName) {
                SIP003_OBFS_LOCAL -> ObfsLocalForm(uiState, viewModel, innerPadding)
                SIP003_V2RAY_PLUGIN -> V2RayPluginForm(uiState, viewModel, innerPadding)
                else -> EmptyForm(innerPadding)
            }
        }
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
                TextButton(stringResource(Res.string.no)) {
                    discardAndExit()
                }
            },
            icon = { Icon(vectorResource(Res.drawable.question_mark), null) },
            title = { Text(stringResource(Res.string.unsaved_changes_prompt)) },
        )
    }
}

@Composable
private fun ObfsLocalForm(
    uiState: SIP003EditorUiState,
    viewModel: SIP003EditorViewModel,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        item("category", PreferenceType.CATEGORY) {
            PreferenceCategory(text = { Text(stringResource(Res.string.plugin)) })
        }
        preferenceGroup {
            ListPreference(
                value = uiState.obfs,
                values = ObfsMode.entries,
                onValueChange = viewModel::setObfs,
                title = { Text(stringResource(Res.string.obfs_mode)) },
                icon = {
                    MaskedIcon(
                        Res.drawable.enhanced_encryption,
                        color = IconMaskColors.IconLightOrange,
                    )
                },
                summary = { Text(uiState.obfs.value) },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = { AnnotatedString(it.value) },
            )
            TextFieldPreference(
                value = uiState.obfsHost,
                onValueChange = viewModel::setObfsHost,
                title = { Text(stringResource(Res.string.http_host)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.router,
                        color = IconMaskColors.IconLightBlue,
                    )
                },
                summary = { Text(contentOrUnset(uiState.obfsHost)) },
                valueToText = { it },
            )
        }
        item("bottom", "padding") {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun V2RayPluginForm(
    uiState: SIP003EditorUiState,
    viewModel: SIP003EditorViewModel,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        item("category", PreferenceType.CATEGORY) {
            PreferenceCategory(text = { Text(stringResource(Res.string.plugin)) })
        }
        preferenceGroup {
            SwitchPreference(
                value = uiState.tls,
                onValueChange = viewModel::setTls,
                title = { Text(stringResource(Res.string.tls)) },
                icon = {
                    MaskedIcon(
                        Res.drawable.security,
                        color = IconMaskColors.IconCoral,
                    )
                },
            )
            ListPreference(
                value = uiState.mode,
                values = V2RayMode.entries,
                onValueChange = viewModel::setMode,
                title = { Text(stringResource(Res.string.v2ray_transport)) },
                icon = {
                    MaskedIcon(
                        Res.drawable.multiple_stop,
                        color = IconMaskColors.IconLightGreen,
                    )
                },
                summary = { Text(uiState.mode.value) },
                type = ListPreferenceType.DROPDOWN_MENU,
                valueToText = { AnnotatedString(it.value) },
            )
            TextFieldPreference(
                value = uiState.host,
                onValueChange = viewModel::setHost,
                title = { Text(stringResource(Res.string.http_host)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.router,
                        color = IconMaskColors.IconLightBlue,
                    )
                },
                summary = { Text(contentOrUnset(uiState.host)) },
                valueToText = { it },
            )
            TextFieldPreference(
                value = uiState.path,
                onValueChange = viewModel::setPath,
                title = { Text(stringResource(Res.string.http_path)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.assistant_direction,
                        color = IconMaskColors.IconLightOrange,
                    )
                },
                summary = { Text(contentOrUnset(uiState.path)) },
                valueToText = { it },
            )
            TextFieldPreference(
                value = uiState.mux,
                onValueChange = viewModel::setMux,
                title = { Text(stringResource(Res.string.mux_number)) },
                textToValue = { it.toIntOrNull() ?: DEFAULT_V2RAY_MUX },
                icon = {
                    MaskedIcon(
                        resource = Res.drawable.numbers,
                        color = IconMaskColors.IconLightYellow,
                        shape = IconMaskShapes.route(),
                    )
                },
                summary = { Text(uiState.mux.toString()) },
                valueToText = { it.toString() },
                textField = { value, onValueChange, onOk ->
                    UIntegerTextField(value, onValueChange, onOk)
                },
            )
            TextFieldPreference(
                value = uiState.certRaw,
                onValueChange = viewModel::setCertRaw,
                title = { Text(stringResource(Res.string.certificates)) },
                textToValue = { it },
                icon = {
                    MaskedIcon(
                        Res.drawable.vpn_key,
                        color = IconMaskColors.IconWarmGray,
                        shape = IconMaskShapes.credential(),
                    )
                },
                summary = { Text(contentOrUnset(uiState.certRaw)) },
                valueToText = { it },
                textField = { value, onValueChange, onOk ->
                    MultilineTextField(value, onValueChange, onOk)
                },
            )
        }
        item("bottom", "padding") {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun EmptyForm(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.sip003_pick_plugin_first))
    }
}
