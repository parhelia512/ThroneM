package io.throneproj.thronem.compose

import androidx.compose.runtime.Composable

@Composable
expect fun rememberVpnServiceLauncher(onFailed: () -> Unit): () -> Unit
