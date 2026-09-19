package io.throneproj.thronem.ui.dashboard

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberOpenProcessAppInfo(process: String?): (() -> Unit)?
