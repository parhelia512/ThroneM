package io.throneproj.thronem.core

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * In-process ring for app-side log lines. Core logs arrive through the
 * command client's log stream; app logs (Logs.ktx) land here so the logcat
 * screen can show both sources.
 */
object LogBuffer {

    private const val MAX_ENTRIES = 3000

    private val entries = ArrayDeque<CoreLogEntry>(MAX_ENTRIES)

    val updates: MutableSharedFlow<CoreLogEntry> = MutableSharedFlow(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    @Synchronized
    fun append(level: CoreLogLevel, message: String) {
        if (entries.size >= MAX_ENTRIES) {
            entries.removeFirst()
        }
        val entry = CoreLogEntry(level, message)
        entries.addLast(entry)
        updates.tryEmit(entry)
    }

    @Synchronized
    fun snapshot(): List<CoreLogEntry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
