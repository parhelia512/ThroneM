package io.throneproj.thronem.ui.dashboard

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLoadPlatformNetworkInfo(): suspend () -> Triple<List<NetworkInterfaceInfo>, String?, String?>
