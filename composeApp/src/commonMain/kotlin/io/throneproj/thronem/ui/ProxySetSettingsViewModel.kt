package io.throneproj.thronem.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.ProxySet
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.internal.ProxySetBean
import io.throneproj.thronem.ktx.applyDefaultValues
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.duplicate_name
import io.throneproj.thronem.resources.error_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

@Immutable
internal sealed interface ProviderUiItem {

    val key: Long

    fun toProvider(): ProxySetBean.Provider

    data class Profile(override val key: Long, val entity: ProxyEntity) : ProviderUiItem {
        override fun toProvider() = ProxySetBean.Provider.Single(entity.id)
    }

    data class Group(
        override val key: Long,
        val groupID: Long,
        val filterNotRegex: String,
    ) : ProviderUiItem {
        override fun toProvider() = ProxySetBean.Provider.Group(groupID, filterNotRegex)
    }
}

@Immutable
internal data class ProxySetSettingsUiState(
    val name: String = "",
    val management: Int = ProxySetBean.MANAGEMENT_SELECTOR,
    val interruptExistConnections: Boolean = false,

    val testURL: String = "",
    val interval: String = "",
    val testIdleTimeout: String = "",
    val testTolerance: Int = 50,

    val providers: ImmutableList<ProviderUiItem> = persistentListOf(),
    val groups: LinkedHashMap<Long, ProxyGroup> = LinkedHashMap(),
)

@Immutable
internal sealed interface ProxySetSettingsUiEvent {
    data class Alert(val title: StringOrRes, val message: StringOrRes) : ProxySetSettingsUiEvent
}

@Stable
internal class ProxySetSettingsViewModel(
    setId: Long,
) : ViewModel() {

    val uiState: StateFlow<ProxySetSettingsUiState>
        field = MutableStateFlow(ProxySetSettingsUiState())

    private val uiEventChannel = Channel<ProxySetSettingsUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<ProxySetSettingsUiEvent> = uiEventChannel.receiveAsFlow()

    private var editingId: Long = 0L
    val editingIdValue: Long get() = editingId
    val isNew get() = editingId == 0L
    private var bean = ProxySetBean()

    private val keyCounter = AtomicLong(0L)
    private fun newItemKey(): Long = keyCounter.incrementAndGet()

    private val initialState = MutableStateFlow<ProxySetSettingsUiState?>(null)
    val isDirty = combine(uiState, initialState) { currentState, initialState ->
        initialState?.let {
            it != currentState
        } ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    init {
        initialize(setId)
    }

    fun initialize(id: Long) = viewModelScope.launch {
        editingId = id
        initialState.value = null
        bean = if (isNew) {
            ProxySetBean().applyDefaultValues()
        } else {
            ThroneDatabase.proxySetDao.getById(id).first()?.bean ?: ProxySetBean().applyDefaultValues()
        }
        bean.writeToUiState()
        load(bean.providers)
        initialState.value = uiState.value
    }

    fun delete() = runOnIoDispatcher {
        ThroneDatabase.proxySetDao.deleteById(editingId)
    }

    fun save() = runOnIoDispatcher {
        bean.loadFromUiState(uiState.value)
        if (isNew) {
            val entity = ProxySet(userOrder = ThroneDatabase.proxySetDao.nextOrder() ?: 1L, bean = bean)
            ThroneDatabase.proxySetDao.createSet(entity)
        } else {
            val entity = ThroneDatabase.proxySetDao.getById(editingId).first() ?: return@runOnIoDispatcher
            entity.bean = bean
            ThroneDatabase.proxySetDao.updateSet(entity)
        }
    }

    private fun ProxySetBean.writeToUiState() {
        uiState.update {
            it.copy(
                name = name,
                management = management,
                interruptExistConnections = interruptExistConnections,
                testURL = testURL,
                interval = interval,
                testIdleTimeout = testIdleTimeout,
                testTolerance = testTolerance,
            )
        }
    }

    private fun ProxySetBean.loadFromUiState(state: ProxySetSettingsUiState) {
        name = state.name
        management = state.management
        interruptExistConnections = state.interruptExistConnections
        testURL = state.testURL
        interval = state.interval
        testIdleTimeout = state.testIdleTimeout
        testTolerance = state.testTolerance
        providers = state.providers.map { it.toProvider() }
    }

    private suspend fun load(providers: List<ProxySetBean.Provider>) {
        val groups = ThroneDatabase.groupDao.allGroups().first()
        val groupMap = LinkedHashMap<Long, ProxyGroup>(groups.size)
        groups.associateByTo(groupMap) { it.id }

        val singleIDs = providers.filterIsInstance<ProxySetBean.Provider.Single>().map { it.id }
        val profiles = ProfileManager.getProfiles(singleIDs).associateBy { it.id }
        val items = withContext(Dispatchers.Default) {
            val items = ArrayList<ProviderUiItem>(providers.size)
            for (provider in providers) {
                when (provider) {
                    is ProxySetBean.Provider.Single -> {
                        val profile = profiles[provider.id] ?: continue
                        items.add(ProviderUiItem.Profile(newItemKey(), profile))
                    }

                    is ProxySetBean.Provider.Group -> items.add(
                        ProviderUiItem.Group(
                            key = newItemKey(),
                            groupID = provider.groupID,
                            filterNotRegex = provider.filterNotRegex,
                        ),
                    )
                }
            }
            items
        }
        uiState.update { state ->
            state.copy(groups = groupMap, providers = items.toImmutableList())
        }
    }

    fun submitReorder(changes: List<OrderedItem<ProviderUiItem>>) {
        invalidateProviderMutation()

        uiState.update { state ->
            val current = state.providers
            val changesMap = changes.associate { it.value.key to it.newIndex }
            val reordered = current.sortedBy { item ->
                changesMap[item.key] ?: current.indexOf(item)
            }
            state.copy(providers = reordered.toImmutableList())
        }
    }

    fun remove(index: Int) {
        invalidateProviderMutation()
        uiState.update { state ->
            if (index !in state.providers.indices) return
            val providers = state.providers.toMutableList()
            providers.removeAt(index)
            state.copy(providers = providers.toImmutableList())
        }
    }

    var replacing = -1

    private var mutationJob: Job? = null
    private var mutationVersion = 0L

    private fun invalidateProviderMutation() {
        mutationVersion++
        mutationJob?.cancel()
        mutationJob = null
    }

    fun onSelectProfile(id: Long) {
        val replacingIndex = replacing
        replacing = -1
        val version = ++mutationVersion
        mutationJob?.cancel()
        mutationJob = viewModelScope.launch {
            val profile = ProfileManager.getProfile(id) ?: return@launch
            if (version != mutationVersion) return@launch
            uiState.update { state ->
                val providers = state.providers.toMutableList()
                if (replacingIndex >= providers.size) return@launch
                val duplicated = providers.filterIndexed { index, item ->
                    index != replacingIndex
                            && item is ProviderUiItem.Profile
                            && item.entity.id == id
                }
                if (duplicated.isNotEmpty()) {
                    emitAlert(
                        title = StringOrRes.Res(Res.string.duplicate_name),
                        message = StringOrRes.Direct(profile.displayName()),
                    )
                    return@launch
                }
                if (replacingIndex < 0) {
                    providers.add(ProviderUiItem.Profile(newItemKey(), profile))
                } else {
                    providers[replacingIndex] =
                        ProviderUiItem.Profile(providers[replacingIndex].key, profile)
                }
                state.copy(providers = providers.toImmutableList())
            }
        }
    }

    fun setName(name: String) {
        uiState.update { it.copy(name = name) }
    }

    fun setManagement(management: Int) {
        uiState.update { it.copy(management = management) }
    }

    fun setInterruptExistConnections(interrupt: Boolean) {
        uiState.update { it.copy(interruptExistConnections = interrupt) }
    }

    fun setTestURL(url: String) {
        uiState.update { it.copy(testURL = url) }
    }

    fun setInterval(interval: String) {
        uiState.update { it.copy(interval = interval) }
    }

    fun setTestIdleTimeout(timeout: String) {
        uiState.update { it.copy(testIdleTimeout = timeout) }
    }

    fun setTestTolerance(tolerance: Int) {
        uiState.update { it.copy(testTolerance = tolerance) }
    }

    fun addGroupProvider(groupID: Long, filterNotRegex: String) {
        submitGroupProvider(index = -1, groupID = groupID, filterNotRegex = filterNotRegex)
    }

    fun setGroupProvider(index: Int, groupID: Long, filterNotRegex: String) {
        submitGroupProvider(index = index, groupID = groupID, filterNotRegex = filterNotRegex)
    }

    private fun submitGroupProvider(index: Int, groupID: Long, filterNotRegex: String) {
        try {
            filterNotRegex.blankAsNull()?.toRegex()
        } catch (error: IllegalArgumentException) {
            invalidateProviderMutation()
            viewModelScope.launch {
                emitAlert(
                    title = StringOrRes.Res(Res.string.error_title),
                    message = StringOrRes.Direct(
                        error.message ?: "Invalid regular expression",
                    ),
                )
            }
            return
        }

        val duplicated = uiState.value.providers.filterIndexed { itemIndex, item ->
            itemIndex != index && item is ProviderUiItem.Group && item.groupID == groupID
        }
        if (duplicated.isNotEmpty()) {
            invalidateProviderMutation()
            viewModelScope.launch {
                emitAlert(
                    title = StringOrRes.Res(Res.string.duplicate_name),
                    message = StringOrRes.Direct(
                        uiState.value.groups[groupID]?.displayName().orEmpty(),
                    ),
                )
            }
            return
        }

        val version = ++mutationVersion
        mutationJob?.cancel()
        mutationJob = viewModelScope.launch {
            if (version != mutationVersion) return@launch
            uiState.update { state ->
                val providers = state.providers.toMutableList()
                if (index in providers.indices) {
                    providers[index] = ProviderUiItem.Group(
                        key = providers[index].key,
                        groupID = groupID,
                        filterNotRegex = filterNotRegex,
                    )
                } else {
                    providers.add(
                        ProviderUiItem.Group(
                            key = newItemKey(),
                            groupID = groupID,
                            filterNotRegex = filterNotRegex,
                        ),
                    )
                }
                state.copy(providers = providers.toImmutableList())
            }
        }
    }

    private suspend fun emitAlert(title: StringOrRes, message: StringOrRes) {
        uiEventChannel.send(ProxySetSettingsUiEvent.Alert(title, message))
    }
}
