package io.throneproj.thronem.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import io.throneproj.thronem.ui.VpnRequestActivity

@Composable
actual fun rememberVpnServiceLauncher(onFailed: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(VpnRequestActivity.StartService()) { failed ->
        if (failed) onFailed()
    }
    return { launcher.launch(null) }
}
