package io.throneproj.thronem.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxySet
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Immutable
data class ProxySetsUiState(
    val sets: List<ProxySet> = emptyList(),
    val selectedSetId: Long = 0L,
)

@Stable
class ProxySetScreenViewModel : ViewModel() {

    val uiState: StateFlow<ProxySetsUiState>
        field = MutableStateFlow(ProxySetsUiState())

    private val selectAccess = Mutex()

    init {
        viewModelScope.launch {
            combine(
                ThroneDatabase.proxySetDao.allSets(),
                DataStore.selectedProxySet.flow(),
            ) { sets, selectedSetId ->
                ProxySetsUiState(sets = sets, selectedSetId = selectedSetId)
            }.collectLatest { state ->
                uiState.value = state
            }
        }
    }

    fun delete(setId: Long) = runOnIoDispatcher {
        ThroneDatabase.proxySetDao.deleteById(setId)
    }

    fun select(new: Long) = viewModelScope.launch {
        var updated: Boolean
        selectAccess.withLock {
            val lastSelected = DataStore.selectedProxySet.get()
            updated = new != lastSelected
            DataStore.selectedProxySet.set(new)
        }
        if (updated) {
            if (DataStore.serviceState.canStop && selectAccess.tryLock()) {
                resolveRepository().reloadService()
                selectAccess.unlock()
            }
        } else if (resolveRepository().isTv) {
            if (DataStore.serviceState.started) {
                resolveRepository().stopService()
            } else {
                resolveRepository().startService()
            }
        }
    }

    fun submitReordered(changes: List<OrderedItem<ProxySet>>) {
        val current = uiState.value.sets
        if (changes.isEmpty() || current.isEmpty()) return
        val changesMap = changes.associate { it.value.id to it.newIndex }
        val reordered = current.sortedBy { item ->
            changesMap[item.id] ?: current.indexOf(item)
        }
        val toUpdate = reordered.mapIndexedNotNull { index, item ->
            val newOrder = (index + 1).toLong()
            if (item.userOrder != newOrder) item.copy(userOrder = newOrder) else null
        }
        if (toUpdate.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                toUpdate.forEach { ThroneDatabase.proxySetDao.updateSet(it) }
            }
        }
    }

}

