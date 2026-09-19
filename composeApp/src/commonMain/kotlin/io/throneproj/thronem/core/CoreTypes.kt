package io.throneproj.thronem.core

import io.throneproj.thronem.bg.ServiceStatus

/**
 * App-native replacements for the generated protobuf types that disappeared
 * together with the gRPC bridge. Everything else on the data plane reuses the
 * libbox model classes (StatusMessage, LogEntry, OutboundGroup, Connection, …)
 * directly.
 */

/** sing-box log level numbering, same values libbox reports on [io.nekohasekai.libbox.LogEntry]. */
enum class CoreLogLevel(val number: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5),
    PANIC(6),
    ;

    companion object {
        fun forNumber(number: Int): CoreLogLevel? = entries.firstOrNull { it.number == number }
    }
}

/** One log line as delivered by the log stream. */
data class CoreLogEntry(
    val level: CoreLogLevel,
    val message: String,
)

/**
 * A batch of log lines. [reset] asks the receiver to drop what it has before
 * appending (fresh connection, or the log buffer was cleared).
 */
data class CoreLogBatch(
    val reset: Boolean,
    val entries: List<CoreLogEntry>,
)

/**
 * Lifecycle status of the service host, mirroring [ServiceStatus].
 */
enum class CoreServiceRunState {
    IDLE,
    STARTING,
    STARTED,
    STOPPING,
    FATAL,
}

data class CoreServiceStatus(
    val state: CoreServiceRunState,
)
