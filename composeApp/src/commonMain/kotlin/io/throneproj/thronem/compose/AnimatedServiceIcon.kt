package io.throneproj.thronem.compose

import androidx.compose.runtime.Composable
import io.throneproj.thronem.bg.ServiceState

@Composable
expect fun AnimatedServiceIcon(state: ServiceState, contentDescription: String)
