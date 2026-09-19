package io.throneproj.thronem.ktx

import android.widget.Toast
import io.throneproj.thronem.repository.resolveAndroidRepository

actual fun showToast(message: String, long: Boolean) {
    val duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    runOnMainDispatcher {
        Toast.makeText(resolveAndroidRepository().context, message, duration).show()
    }
}
