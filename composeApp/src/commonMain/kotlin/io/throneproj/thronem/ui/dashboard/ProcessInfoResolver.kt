package io.throneproj.thronem.ui.dashboard

internal expect class ProcessInfoResolver() {
    suspend fun resolve(process: String?, uid: Int): ProcessInfo?
    fun clear()
}
