package io.throneproj.thronem.ui.profile

import androidx.compose.runtime.Composable
import io.throneproj.thronem.database.ProxyEntity

@Composable
internal expect fun platformSupportShortcut(): Boolean

@Composable
internal expect fun ShortcutMenuItem(entity: ProxyEntity, postClick: () -> Unit)