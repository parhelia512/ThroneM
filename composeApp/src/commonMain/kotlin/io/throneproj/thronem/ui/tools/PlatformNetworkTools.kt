package io.throneproj.thronem.ui.tools

import androidx.compose.runtime.Composable
import io.throneproj.thronem.ui.NavRoutes

@Composable
internal expect fun PlatformNetworkTools(onOpenTool: (NavRoutes.ToolsPage) -> Unit)
