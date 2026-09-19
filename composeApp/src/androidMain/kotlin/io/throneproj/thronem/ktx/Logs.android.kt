package io.throneproj.thronem.ktx

import android.util.Log
import io.throneproj.thronem.core.CoreLogLevel
import io.throneproj.thronem.core.LogBuffer

actual object Logs {

    private fun mkTag(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace[4].className.substringAfterLast(".")
    }

    private fun logToAndroid(level: Int, tag: String, message: String) {
        when (level) {
            Log.DEBUG -> Log.d(tag, message)
            Log.INFO -> Log.i(tag, message)
            Log.WARN -> Log.w(tag, message)
            Log.ERROR -> Log.e(tag, message)
            else -> Log.println(level, tag, message)
        }
    }

    private fun record(level: CoreLogLevel, tag: String, message: String) {
        logToAndroid(level.toAndroidLevel(), tag, message)
        LogBuffer.append(level, "[$tag] $message")
    }

    private fun CoreLogLevel.toAndroidLevel(): Int = when (this) {
        CoreLogLevel.TRACE, CoreLogLevel.DEBUG -> Log.DEBUG
        CoreLogLevel.INFO -> Log.INFO
        CoreLogLevel.WARN -> Log.WARN
        else -> Log.ERROR
    }

    actual fun d(message: String) {
        record(CoreLogLevel.DEBUG, mkTag(), message)
    }

    actual fun d(message: String, exception: Throwable) {
        record(CoreLogLevel.DEBUG, mkTag(), "$message\n${exception.stackTraceToString()}")
    }

    actual fun i(message: String) {
        record(CoreLogLevel.INFO, mkTag(), message)
    }

    actual fun i(message: String, exception: Throwable) {
        record(CoreLogLevel.INFO, mkTag(), "$message\n${exception.stackTraceToString()}")
    }

    actual fun w(message: String) {
        record(CoreLogLevel.WARN, mkTag(), message)
    }

    actual fun w(message: String, exception: Throwable) {
        record(CoreLogLevel.WARN, mkTag(), "$message\n${exception.stackTraceToString()}")
    }

    actual fun w(exception: Throwable) {
        record(CoreLogLevel.WARN, mkTag(), exception.stackTraceToString())
    }

    actual fun e(message: String) {
        record(CoreLogLevel.ERROR, mkTag(), message)
    }

    actual fun e(message: String, exception: Throwable) {
        record(CoreLogLevel.ERROR, mkTag(), "$message\n${exception.stackTraceToString()}")
    }

    actual fun e(exception: Throwable) {
        record(CoreLogLevel.ERROR, mkTag(), exception.stackTraceToString())
    }

}
