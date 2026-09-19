package io.throneproj.thronem.utils

import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.fileNameTimestamp
import java.io.File
import java.io.FileInputStream
import java.io.IOException

internal expect fun dumpPlatformLogcat(): String

data class LogExport(
    val fileName: String,
    val content: String,
)

object SendLog {

    private const val LOCAL_TARGET_NAME = "local"
    private const val FILE_NAME_PREFIX = "thronem"
    private const val FILE_NAME_EXTENSION = ".log"
    private const val CORE_LOG_FILE_NAME = "stderr.log"
    private val unsafeFileNameCharacters = Regex("[^A-Za-z0-9._-]")

    fun buildLocalLog(externalAssetsDir: File): LogExport = LogExport(
        fileName = buildFileName(LOCAL_TARGET_NAME),
        content = buildString {
            append(CrashReport.buildReportHeader())
            appendLine("Logcat: ")
            appendLine()
            try {
                appendLine(dumpPlatformLogcat())
            } catch (e: IOException) {
                Logs.w(e)
                appendLine("Export logcat error: " + CrashReport.formatThrowable(e))
            }
            appendLine(getCoreLog(externalAssetsDir))
        },
    )

    private fun buildFileName(targetName: String): String {
        val safeName = targetName.replace(unsafeFileNameCharacters, "-")
        return "$FILE_NAME_PREFIX-$safeName-${fileNameTimestamp()}$FILE_NAME_EXTENSION"
    }

    private fun getCoreLog(externalAssetsDir: File): String {
        return try {
            val logFile = externalAssetsDir.resolve(CORE_LOG_FILE_NAME)
            val stream = FileInputStream(logFile)
            stream.use { it.readBytes() }.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            e.stackTraceToString()
        }
    }
}
