package io.throneproj.thronem.ui.tools

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import io.throneproj.thronem.ktx.runOnMainDispatcher
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.share
import java.io.File

@Composable
internal actual fun rememberShareBackupFile(): (File) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { file ->
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.cache",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND)
                .setType("application/json")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, fileUri)

            runOnMainDispatcher {
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        resolveRepository().getString(Res.string.share),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
