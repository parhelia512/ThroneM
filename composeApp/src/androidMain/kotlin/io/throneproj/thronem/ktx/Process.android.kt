package io.throneproj.thronem.ktx

import android.content.Intent
import com.jakewharton.processphoenix.ProcessPhoenix
import io.throneproj.thronem.repository.resolveAndroidRepository

actual fun restartApplication() {
    ProcessPhoenix.triggerRebirth(
        resolveAndroidRepository().context,
        Intent(resolveAndroidRepository().context, Class.forName("io.throneproj.thronem.ui.MainActivity")),
    )
}

actual fun exitApplication() {
    android.os.Process.killProcess(android.os.Process.myPid())
}
