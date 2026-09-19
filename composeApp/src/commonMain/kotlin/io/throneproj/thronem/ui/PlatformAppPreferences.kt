package io.throneproj.thronem.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun AppSelectPreference(
    packages: Set<String>,
    onSelectApps: (Set<String>) -> Unit,
)
