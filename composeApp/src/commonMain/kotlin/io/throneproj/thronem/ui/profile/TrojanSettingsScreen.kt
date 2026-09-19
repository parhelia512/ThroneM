package io.throneproj.thronem.ui.profile

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import io.throneproj.thronem.compose.PasswordPreference
import io.throneproj.thronem.compose.preferenceGroup
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.profile_config
import io.throneproj.thronem.ui.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrojanSettingsScreen(
    profileId: Long,
    isSubscription: Boolean,
    onResult: (updated: Boolean) -> Unit,
    onOpenConfigEditor: (NavRoutes.ConfigEditor) -> Unit,
) {
    val viewModel: TrojanSettingsViewModel = profileEditorViewModel(
        profileId = profileId,
        isSubscription = isSubscription,
    ) {
        TrojanSettingsViewModel()
    }

    ProfileSettingsScreenScaffold(
        title = Res.string.profile_config,
        viewModel = viewModel,
        onResult = onResult,
        onOpenConfigEditor = onOpenConfigEditor,
    ) { uiState, scrollTo ->
        trojanSettings(uiState as TrojanUiState, viewModel, scrollTo)
    }
}


private fun LazyListScope.trojanSettings(
    uiState: TrojanUiState,
    viewModel: TrojanSettingsViewModel,
    scrollTo: (String) -> Unit,
) {
    headSettings(uiState, viewModel)
    preferenceGroup(key = "password") {
        PasswordPreference(
            value = uiState.password,
            onValueChange = { viewModel.setPassword(it) },
        )
    }
    transportSettings(uiState, viewModel)
    muxSettings(uiState, viewModel)
    tlsSettings(uiState, viewModel, scrollTo)
}
