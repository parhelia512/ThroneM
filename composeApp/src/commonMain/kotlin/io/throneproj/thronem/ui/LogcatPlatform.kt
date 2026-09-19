package io.throneproj.thronem.ui

import androidx.compose.runtime.Composable
import io.throneproj.thronem.utils.LogExport
import kotlinx.coroutines.CoroutineScope

@Composable
internal expect fun ShareActionRow(
    scope: CoroutineScope,
    buildLog: suspend () -> LogExport,
    showSnackbar: suspend (Exception) -> Unit,
)
