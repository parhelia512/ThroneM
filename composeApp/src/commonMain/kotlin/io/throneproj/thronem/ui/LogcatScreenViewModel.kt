package io.throneproj.thronem.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.core.CoreLogBatch
import io.throneproj.thronem.core.CoreLogEntry
import io.throneproj.thronem.core.CoreLogLevel
import io.throneproj.thronem.core.LogBuffer
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.utils.LogExport
import io.throneproj.thronem.utils.SendLog
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

private const val MAX_LOG_ENTRIES = 3000

private const val LOG_TRIM_SLACK = 500

private const val UI_STATE_STOP_TIMEOUT = 5000L

@Immutable
data class LogcatUiState(
    val pause: Boolean = false,
    val searchQuery: String? = null,
    val logLevel: CoreLogLevel = CoreLogLevel.WARN,
    val logs: PersistentList<LogEntry> = persistentListOf(),
    val errorMessage: String? = null,
    val connecting: Boolean = false,
)

val logLevels: List<CoreLogLevel> = CoreLogLevel.entries

@Immutable
data class LogEntry(
    val level: CoreLogLevel,
    val message: String,
)

fun CoreLogEntry.toLogEntry(): LogEntry = LogEntry(
    level = level,
    message = message,
)

@Immutable
private data class LogcatFilter(
    val logLevel: CoreLogLevel,
    val searchQuery: String? = null,
) {
    fun accepts(entry: LogEntry): Boolean {
        if (entry.level.number > logLevel.number) return false
        return searchQuery == null || entry.message.contains(searchQuery, ignoreCase = true)
    }
}

@Immutable
private data class LogcatStatus(
    val errorMessage: String? = null,
    val connecting: Boolean = false,
)

private fun PersistentList<LogEntry>.appendBounded(
    entries: List<LogEntry>,
): PersistentList<LogEntry> {
    val appended = addingAll(entries)
    if (appended.size <= MAX_LOG_ENTRIES + LOG_TRIM_SLACK) return appended
    return appended.subList(appended.size - MAX_LOG_ENTRIES, appended.size).toPersistentList()
}

private fun buildUiState(
    liveLogs: PersistentList<LogEntry>,
    pausedLogs: PersistentList<LogEntry>?,
    filter: LogcatFilter,
    status: LogcatStatus,
): LogcatUiState {
    val displayed = pausedLogs ?: liveLogs
    return LogcatUiState(
        pause = pausedLogs != null,
        searchQuery = filter.searchQuery,
        logLevel = filter.logLevel,
        logs = displayed.filter(filter::accepts).toPersistentList(),
        errorMessage = status.errorMessage,
        connecting = status.connecting,
    )
}

@Stable
class LogcatScreenViewModel(
    coreClient: CoreClient? = null,
) : ViewModel() {
    private val coreClientOverride = coreClient

    private val coreClient: CoreClient
        get() = coreClientOverride
            ?: GlobalContext.get().get()

    private val localLogLevel: CoreLogLevel
        get() = CoreLogLevel.forNumber(DataStore.logLevel.getBlocking()) ?: CoreLogLevel.WARN

    private val liveLogs = MutableStateFlow(persistentListOf<LogEntry>())

    private val pausedLogs = MutableStateFlow<PersistentList<LogEntry>?>(null)

    private val filter = MutableStateFlow(LogcatFilter(logLevel = localLogLevel))
    private val status = MutableStateFlow(LogcatStatus())

    val uiState: StateFlow<LogcatUiState> = combine(
        liveLogs,
        pausedLogs,
        filter,
        status,
        ::buildUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(UI_STATE_STOP_TIMEOUT),
        initialValue = buildUiState(
            liveLogs = liveLogs.value,
            pausedLogs = pausedLogs.value,
            filter = filter.value,
            status = status.value,
        ),
    )

    val searchTextFieldState = TextFieldState()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            snapshotFlow { searchTextFieldState.text.toString() }
                .drop(1)
                .distinctUntilChanged()
                .collect { setSearchQuery(it.ifEmpty { null }) }
        }
    }

    suspend fun buildExportLog(): LogExport {
        return withContext(Dispatchers.IO) {
            SendLog.buildLocalLog(resolveRepository().externalAssetsDir)
        }
    }

    suspend fun initialize(isConnected: Boolean) {
        job?.cancel()
        liveLogs.value = persistentListOf()
        pausedLogs.value = null
        filter.update { it.copy(logLevel = localLogLevel) }
        status.value = LogcatStatus(
            connecting = !isConnected,
        )
        if (!isConnected) return

        job = viewModelScope.launch {
            // Seed app-side log ring first, then keep merging both sources.
            liveLogs.update { current ->
                current.appendBounded(LogBuffer.snapshot().map { it.toLogEntry() })
            }
            launch {
                LogBuffer.updates.collect { entry ->
                    liveLogs.update { current -> current.appendBounded(listOf(entry.toLogEntry())) }
                }
            }
            try {
                coreClient.subscribeLog().collect { batch ->
                    if (batch.reset) clearLogBuffers()
                    val entries = batch.entries.map { it.toLogEntry() }
                    liveLogs.update { it.appendBounded(entries) }
                }
            } catch (e: Exception) {
                Logs.w("subscribe logs", e)
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }

    private fun clearLogBuffers() {
        liveLogs.value = persistentListOf()
        pausedLogs.update { snapshot -> snapshot?.let { persistentListOf() } }
    }

    fun togglePause() {
        pausedLogs.update { snapshot -> if (snapshot == null) liveLogs.value else null }
    }

    fun clearLog() = viewModelScope.launch(Dispatchers.IO) {
        try {
            coreClient.clearLogs()
            LogBuffer.clear()
        } catch (e: Exception) {
            Logs.w("clear log", e)
        }
        clearLogBuffers()
    }

    fun setLogLevel(level: CoreLogLevel) {
        filter.update { it.copy(logLevel = level) }
    }

    fun setSearchQuery(query: String?) {
        filter.update { it.copy(searchQuery = query) }
    }

    fun clearSearchQuery() {
        searchTextFieldState.setTextAndPlaceCursorAtEnd("")
    }

}

