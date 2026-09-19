package io.throneproj.thronem.utils

internal actual fun dumpPlatformLogcat(): String {
    return Runtime.getRuntime()
        .exec(arrayOf("logcat", "-d"))
        .inputStream.bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
}
