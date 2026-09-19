package io.throneproj.thronem.utils

import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.extractGzipFileTo
import java.io.File

private const val BUNDLED_RULE_SET_BASE = "composeResources/io.throneproj.thronem.resources/files/sing-box"
private val RULE_SET_NAMES = listOf("geoip", "geosite")

internal expect suspend fun copyBundledRuleSetAssetsIfNeeded()

internal suspend fun syncBundledRuleSetAssets(
    targetDir: File,
    readResourceBytes: suspend (String) -> ByteArray?,
    copyResource: suspend (String, File) -> Boolean,
) {
    targetDir.mkdirs()

    for (name in RULE_SET_NAMES) {
        val versionPath = "$BUNDLED_RULE_SET_BASE/$name.version.txt"
        val archivePath = "$BUNDLED_RULE_SET_BASE/$name.db.gz"

        val versionBytes = readResourceBytes(versionPath) ?: continue
        val versionFile = File(targetDir, "$name.version.txt")
        val archiveFile = File(targetDir, "$name.db.gz")

        val existingVersion = if (versionFile.isFile) {
            runCatching { versionFile.readBytes() }.getOrNull()
        } else {
            null
        }
        val shouldCopy = existingVersion == null ||
            !archiveFile.isFile ||
            !existingVersion.contentEquals(versionBytes)
        if (!shouldCopy) continue

        if (!copyResource(archivePath, archiveFile)) continue

        try {
            versionFile.writeBytes(versionBytes)
        } catch (e: Exception) {
            Logs.e("Failed to write bundled asset version $name", e)
        }
    }
}

/**
 * Unpacks the bundled geoip.db.gz / geosite.db.gz (copied into [bundledDir] by
 * [copyBundledRuleSetAssetsIfNeeded]) into [workingDir], where the core
 * resolves its default geo paths. Replaces the old `Libcore.extractAssets()`.
 */
internal fun extractBundledGeoAssets(bundledDir: File, workingDir: File) {
    for (name in RULE_SET_NAMES) {
        val archive = File(bundledDir, "$name.db.gz")
        if (!archive.isFile) continue
        val target = File(workingDir, "$name.db")
        archive.extractGzipFileTo(target)
        Logs.d("extracted bundled $name.db to ${target.absolutePath}")
    }
}
