package io.throneproj.thronem.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberShouldRequestBatteryOptimizations(): Boolean

@Composable
internal expect fun rememberRequestIgnoreBatteryOptimizations(): () -> Unit
