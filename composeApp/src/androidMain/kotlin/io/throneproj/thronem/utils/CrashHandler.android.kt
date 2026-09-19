package io.throneproj.thronem.utils

import android.content.Intent
import android.util.Log
import com.jakewharton.processphoenix.ProcessPhoenix
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.repository.resolveAndroidRepository

actual object CrashHandler : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // note: libc / go panic is in android log

        runCatching {
            Log.e(thread.toString(), throwable.stackTraceToString())
        }

        runCatching {
            Logs.e(thread.toString())
            Logs.e(throwable.stackTraceToString())
        }

        ProcessPhoenix.triggerRebirth(
            resolveAndroidRepository().context,
            Intent(resolveAndroidRepository().context, Class.forName("io.throneproj.thronem.ui.BlankActivity"))
                .putExtra("log_title", "thronem_crash"),
        )
    }
}
