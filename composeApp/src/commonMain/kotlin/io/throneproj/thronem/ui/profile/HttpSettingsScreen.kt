package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.throneproj.thronem.compose.MultilineTextField
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.IconMaskColors
import io.throneproj.thronem.compose.MaskedIcon
import io.throneproj.thronem.compose.TextFieldPreference
import io.throneproj.thronem.compose.material3.Text
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.ktx.contentOrUnset
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.code
import io.throneproj.thronem.resources.http_headers
import io.throneproj.thronem.resources.http_host
import io.throneproj.thronem.resources.http_path
import io.throneproj.thronem.resources.language
import io.throneproj.thronem.resources.password
import io.throneproj.thronem.resources.password_opt
import io.throneproj.thronem.resources.person
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.resources.route
import io.throneproj.thronem.resources.username_opt
import io.throneproj.thronem.ui.NavRoutes
import org.jetbrains.compose.resources.stringResource

@Composable
fun HttpSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: HttpSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        HttpSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, scrollTo ->
        httpSettings(uiState as HttpUiState, viewModel, scrollTo)
    }
}

private fun LazyListScope.httpSettings(
    uiState: HttpUiState,
    viewModel: HttpSettingsViewModel,
    scrollTo: (String) -> Unit,
) {
    headSettings(uiState, viewModel)
    preferenceGroup {
        TextFieldPreference(
            value = uiState.username,
            onValueChange = { viewModel.setUsername(it) },
            title = { Text(stringResource(Res.string.username_opt)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.person, color = IconMaskColors.IconCyan)
            },
            summary = { Text(contentOrUnset(uiState.username)) },
            valueToText = { it },
        )
        PasswordPreference(
            value = uiState.password,
            onValueChange = { viewModel.setPassword(it) },
            title = { Text(stringResource(Res.string.password_opt)) },
            icon = {
                MaskedIcon(
                    Res.drawable.password,
                    color = IconMaskColors.IconWarmGray,
                )
            },
        )
    }
    preferenceGroup {
        TextFieldPreference(
            value = uiState.host,
            onValueChange = { viewModel.setHost(it) },
            title = { Text(stringResource(Res.string.http_host)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.language,
                    color = IconMaskColors.IconLightBlue,
                )
            },
            summary = { Text(contentOrUnset(uiState.host)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.path,
            onValueChange = { viewModel.setPath(it) },
            title = { Text(stringResource(Res.string.http_path)) },
            textToValue = { it },
            icon = {
                MaskedIcon(
                    Res.drawable.route,
                    color = IconMaskColors.IconLightOrange,
                )
            },
            summary = { Text(contentOrUnset(uiState.path)) },
            valueToText = { it },
        )
        TextFieldPreference(
            value = uiState.headers,
            onValueChange = { viewModel.setHeaders(it) },
            title = { Text(stringResource(Res.string.http_headers)) },
            textToValue = { it },
            icon = {
                MaskedIcon(Res.drawable.code, color = IconMaskColors.IconLavender)
            },
            summary = { Text(contentOrUnset(uiState.headers)) },
            valueToText = { it },
            textField = { value, onValueChange, onOk ->
                MultilineTextField(value, onValueChange, onOk)
            },
        )
    }

    tlsSettings(uiState, viewModel, scrollTo)
}
