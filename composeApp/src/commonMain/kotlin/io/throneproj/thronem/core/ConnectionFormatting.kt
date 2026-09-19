package io.throneproj.thronem.core

import io.nekohasekai.libbox.Connection
import io.nekohasekai.libbox.ConnectionEvent
import io.nekohasekai.libbox.Libbox
import io.throneproj.thronem.ktx.emptyAsNull
import io.throneproj.thronem.ktx.toList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** "$name/$type" label composition previously done in Go generateBound. */
fun formatBound(name: String, type: String): String {
    if (name.isEmpty()) return type
    if (type.isEmpty()) return name
    return "$name/$type"
}

fun Connection.inboundLabel(): String = formatBound(inbound, inboundType)

fun Connection.outboundLabel(): String = formatBound(outbound, outboundType)

/** Matched outbound is the last chain hop, else the direct outbound tag. */
fun Connection.matchedOutbound(): String =
    chain().toList().lastOrNull()?.takeIf { it.isNotEmpty() } ?: outbound

/** Rule text; unmatched falls back to "final" (D-P1.8). */
fun Connection.matchedRuleOrFinal(): String =
    rule.ifEmpty { "final" }

fun Connection.chainLabel(): String =
    chain().toList().joinToString(" => ")

/**
 * Formats a unix-millis timestamp as local `yyyy-MM-dd HH:mm:ss`.
 * Zero → empty string.
 */
fun formatConnectionTime(millis: Long): String {
    if (millis <= 0L) return ""
    val dateTime = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return CONNECTION_TIME_FORMAT.format(dateTime)
}

private val CONNECTION_TIME_FORMAT = LocalDateTime.Format {
    // yyyy-MM-dd HH:mm:ss
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun proxyDisplayName(type: String): String = Libbox.proxyDisplayType(type)

fun ConnectionEvent.isNew(): Boolean = type.toLong() == Libbox.ConnectionEventNew

fun ConnectionEvent.isUpdate(): Boolean = type.toLong() == Libbox.ConnectionEventUpdate

fun ConnectionEvent.isClosed(): Boolean = type.toLong() == Libbox.ConnectionEventClosed

/** Process paths / package names used by the dashboard connection detail UI. */
fun Connection.processNames(): List<String>? {
    val info = processInfo ?: return null
    val packages = info.packageNames().toList()
    if (packages.isNotEmpty()) return packages
    return info.processPath.emptyAsNull()?.let { listOf(it) }
}

/**
 * UID for package-based process info, otherwise process id (desktop path).
 */
fun Connection.processUid(): Int {
    val info = processInfo ?: return -1
    return if (info.packageNames().toList().isNotEmpty()) {
        info.userID
    } else {
        info.processID.toInt()
    }
}
