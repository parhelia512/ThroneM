@file:OptIn(ExperimentalCoroutinesApi::class)

package io.throneproj.thronem.ui.configuration

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.throneproj.thronem.bg.RunningConfigRegistry
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.GroupManager
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.Deduplication
import io.throneproj.thronem.fmt.buildConfig
import io.throneproj.thronem.group.RawUpdater
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.not_connected
import io.throneproj.thronem.ktx.SubscriptionFoundException
import io.throneproj.thronem.ktx.isIpAddress
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.ktx.removeFirstMatched
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.ktx.selectByNetworkStrategy
import io.throneproj.thronem.ktx.serverAddressDomainStrategy
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.repository.resolveRepository
import io.github.vinceglb.filekit.PlatformFile
import org.koin.core.context.GlobalContext
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

@Immutable
sealed interface KeyAction {
    data object Consumed : KeyAction
    data object Unhandled : KeyAction
    data object ImportClipboard : KeyAction
    data class SwitchTab(val delta: Int) : KeyAction
    data object OpenSearch : KeyAction
}

@Immutable
data class ConfigurationUiState(
    val groups: List<ProxyGroup> = emptyList(),
    val testState: ConfigurationTestUiState? = null,
    val alertForDelete: AlertForDelete? = null,
)

@Immutable
data class AlertForDelete(
    val size: Int,
    val summary: String,
    val confirm: () -> Unit,
)

@Immutable
data class ConfigurationTestUiState(
    val latestResult: ProfileTestResult? = null,
    val processedCount: Int = 0,
    val total: Int = 0,
)

@Immutable
data class ProfileTestResult(
    val profile: ProxyEntity,
    val result: TestResult,
)

@Stable
sealed interface TestResult {
    data class Success(val ping: Int) : TestResult
    data class Failure(val reason: FailureReason) : TestResult
}

@Stable
sealed interface FailureReason {
    object InvalidConfig : FailureReason
    data class Generic(val message: String?) : FailureReason
}

@Stable
enum class TestType {
    URLTest,
}

@Stable
class ConfigurationScreenViewModel(
    private val coreClient: CoreClient = GlobalContext.get().get(),
) : ViewModel() {


    val uiState: StateFlow<ConfigurationUiState>
        field = MutableStateFlow(ConfigurationUiState())

    val selectedGroup = DataStore.selectedGroup.flow()

    internal val childViewModels = mutableMapOf<Long, GroupProfilesHolderViewModel>()
    private val testErrorMessages = ConcurrentHashMap<Long, String>()

    fun registerChild(groupId: Long, vm: GroupProfilesHolderViewModel) {
        childViewModels[groupId] = vm
        vm.query = searchTextFieldState.text.toString()
    }

    fun unregisterChild(groupId: Long) {
        childViewModels.remove(groupId)
    }

    fun handleKeyAction(
        key: ComposeKey,
        isCtrl: Boolean,
        isShift: Boolean,
        isSearchActive: Boolean,
    ): KeyAction {
        return when {
            key == ComposeKey.Enter && !isCtrl -> {
                if (!DataStore.serviceState.started) {
                    resolveRepository().startService()
                }
                KeyAction.Consumed
            }

            isCtrl && !isShift && key == ComposeKey.S -> {
                if (DataStore.serviceState.canStop) {
                    resolveRepository().stopService()
                }
                KeyAction.Consumed
            }

            key == ComposeKey.F5 -> {
                if (DataStore.serviceState.started) {
                    resolveRepository().reloadService()
                }
                KeyAction.Consumed
            }

            isCtrl && !isShift && key == ComposeKey.U -> {
                viewModelScope.launch {
                    doTest(DataStore.currentGroupId(), TestType.URLTest)
                }
                KeyAction.Consumed
            }

            key == ComposeKey.Escape -> {
                if (uiState.value.testState != null) {
                    cancelTest()
                    KeyAction.Consumed
                } else {
                    KeyAction.Unhandled
                }
            }

            isCtrl && key == ComposeKey.V -> KeyAction.ImportClipboard

            !isSearchActive && !isCtrl && !isShift && key == ComposeKey.Slash -> {
                KeyAction.OpenSearch
            }

            !isSearchActive && isCtrl && !isShift && key == ComposeKey.F -> {
                KeyAction.OpenSearch
            }

            !isSearchActive && !isCtrl && key == ComposeKey.H -> {
                KeyAction.SwitchTab(-1)
            }

            !isSearchActive && !isCtrl && key == ComposeKey.L -> {
                KeyAction.SwitchTab(1)
            }

            else -> KeyAction.Unhandled
        }
    }

    fun scrollToProxy(proxyId: Long) = viewModelScope.launch {
        val group = withContext(Dispatchers.IO) {
            ProfileManager.getProfile(proxyId)?.groupId
        } ?: return@launch
        childViewModels[group]?.scrollToProxy(proxyId, true)
    }

    private var testJob: Job? = null

    val searchTextFieldState = TextFieldState()

    init {
        viewModelScope.launch {
            snapshotFlow { searchTextFieldState.text.toString() }
                .drop(1)
                .distinctUntilChanged()
                .collect { query ->
                    childViewModels.values.forEach { it.query = query }
                }
        }
    }

    fun clearSearchQuery() {
        searchTextFieldState.setTextAndPlaceCursorAtEnd("")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun doTest(group: Long, type: TestType) {
        val performTest: suspend (ProxyEntity) -> TestResult = when (type) {
            TestType.URLTest -> ::urlTest
        }

        testErrorMessages.clear()
        testJob = viewModelScope.launch {
            val proxies = ThroneDatabase.proxyDao.getByGroup(group).first()
            val totalCount = proxies.size
            var processedCount = 0
            val concurrent = DataStore.connectionTestConcurrent.get()

            if (proxies.isEmpty()) {
                uiState.update { state -> state.copy(testState = null) }
                return@launch
            }

            uiState.update { state ->
                state.copy(
                    testState = ConfigurationTestUiState(
                        total = proxies.size,
                    ),
                )
            }
            val results = mutableListOf<ProfileTestResult>()

            try {
                proxies.asFlow()
                    .flatMapMerge(concurrent) { profile ->
                        flow {
                            val result = withContext(Dispatchers.IO) { performTest(profile) }
                            emit(ProfileTestResult(profile, result))
                        }
                    }
                    .flowOn(Dispatchers.Default)
                    .collect { profileResult ->
                        results.add(profileResult)
                        processedCount++
                        uiState.update { state ->
                            state.copy(
                                testState = ConfigurationTestUiState(
                                    latestResult = profileResult,
                                    processedCount = processedCount,
                                    total = totalCount,
                                ),
                            )
                        }
                    }
            } finally {
                saveResultsAndFinish(results)
            }
        }
    }

    fun rememberDisplayedError(profileId: Long, message: String?) {
        if (message.isNullOrEmpty()) {
            testErrorMessages.remove(profileId)
        } else {
            testErrorMessages[profileId] = message
        }
    }

    private fun saveResultsAndFinish(results: List<ProfileTestResult>) {
        viewModelScope.launch(Dispatchers.IO) {
            val displayedErrors = testErrorMessages.toMap()
            results.forEach {
                try {
                    when (val result = it.result) {
                        is TestResult.Success -> {
                            it.profile.ping = result.ping
                            it.profile.status = ProxyEntity.STATUS_AVAILABLE
                            it.profile.error = null
                        }

                        is TestResult.Failure -> {
                            it.profile.ping = 0

                            it.profile.status = when (result.reason) {
                                FailureReason.InvalidConfig ->
                                    ProxyEntity.STATUS_INVALID

                                is FailureReason.Generic -> ProxyEntity.STATUS_UNAVAILABLE
                            }

                            val displayedError = displayedErrors[it.profile.id]
                            it.profile.error = when (result.reason) {
                                is FailureReason.Generic -> displayedError ?: result.reason.message
                                else -> displayedError
                            }
                        }
                    }
                    ProfileManager.updateProfile(it.profile)
                } catch (e: Exception) {
                    Logs.e(e)
                }
            }

            withContext(Dispatchers.Default) {
                uiState.update { state -> state.copy(testState = null) }
            }
            testErrorMessages.clear()
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        uiState.update { state ->
            state.copy(testState = null)
        }
    }

    private suspend fun urlTest(profile: ProxyEntity): TestResult {
        val testURL = DataStore.connectionTestURL.get()
        val testTimeout = DataStore.connectionTestTimeout.get()

        // libbox cannot run a throwaway config, so the profile must be part of
        // the config the core is currently running.
        val tag = RunningConfigRegistry.metadata?.tagToID
            ?.filterValues { it == profile.id }
            ?.keys
            ?.firstOrNull()
            ?: return TestResult.Failure(
                FailureReason.Generic(resolveRepository().getString(Res.string.not_connected)),
            )

        return try {
            val result = coreClient.urlTest(tag, testURL, testTimeout)
            TestResult.Success(result)
        } catch (e: Exception) {
            TestResult.Failure(FailureReason.Generic(e.readableMessage))
        }
    }

    init {
        viewModelScope.launch {
            ProfileManager.getGroups()
                .flatMapLatest { groups ->
                    val ungroupedGroup = groups.find { it.ungrouped }
                    if (ungroupedGroup != null) {
                        ThroneDatabase.proxyDao.countByGroup(ungroupedGroup.id).map { groups }
                    } else {
                        flowOf(groups)
                    }
                }
                .collectLatest { groups ->
                    reloadGroups(groups)
                }
        }
    }

    private suspend fun reloadGroups(all: List<ProxyGroup>?) {
        val groups = (all ?: withContext(Dispatchers.IO) {
            ProfileManager.getGroups().first()
        }).toMutableList()
        if (groups.size > 1) groups.removeFirstMatched {
            it.ungrouped && ThroneDatabase.proxyDao.countByGroup(it.id).first() == 0L
        }

        if (groups.isNotEmpty()) {
            val selectedId = DataStore.currentGroupId()
            val selectIndex = groups.indexOfFirst { it.id == selectedId }
            if (selectIndex < 0) {
                DataStore.selectedGroup.set(groups[0].id)
            }
        }
        uiState.emit(
            uiState.value.copy(
                groups = groups,
            ),
        )
    }

    fun updateOrder(groupId: Long, order: Int) = viewModelScope.launch {
        val group = uiState.value.groups.find { it.id == groupId } ?: return@launch
        if (group.order == order) return@launch
        withContext(Dispatchers.IO) {
            GroupManager.updateGroup(
                group.copy(
                    order = order,
                ),
            )
        }
    }

    fun clearTrafficStatistics(groupId: Long) = viewModelScope.launch {
        val profiles = withContext(Dispatchers.IO) { ThroneDatabase.proxyDao.getByGroup(groupId).first() }
        val toClear = profiles.mapNotNull {
            if (it.tx != 0L || it.rx != 0L) {
                it.tx = 0L
                it.rx = 0L
                it
            } else {
                null
            }
        }
        if (toClear.isNotEmpty()) withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.updateProxy(toClear)
        }
    }

    fun clearResults(groupId: Long) = viewModelScope.launch {
        val profiles = withContext(Dispatchers.IO) { ThroneDatabase.proxyDao.getByGroup(groupId).first() }
        val toClear = profiles.mapNotNull {
            if (it.status != ProxyEntity.STATUS_INITIAL) {
                it.status = ProxyEntity.STATUS_INITIAL
                it.ping = 0
                it.error = null
                it
            } else {
                null
            }
        }
        if (toClear.isNotEmpty()) withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.updateProxy(toClear)
        }
    }

    fun deleteUnavailable(groupId: Long) = viewModelScope.launch {
        val toDelete = withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.getByGroup(groupId).first().mapNotNull {
                when (it.status) {
                    ProxyEntity.STATUS_INITIAL, ProxyEntity.STATUS_AVAILABLE -> null
                    else -> it
                }
            }
        }
        if (toDelete.isEmpty()) return@launch

        val ids = toDelete.map { it.id }
        uiState.update { state ->
            state.copy(
                alertForDelete = AlertForDelete(
                    size = toDelete.size,
                    summary = nameSummary(toDelete),
                    confirm = {
                        dismissAlert()
                        runOnIoDispatcher {
                            ProfileManager.deleteProfiles(groupId, ids)
                        }
                    },
                ),
            )
        }
    }

    fun removeDuplicate(groupId: Long) = viewModelScope.launch {
        val profiles = withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.getByGroup(groupId).first()
        }
        val uniqueProxies = LinkedHashSet<Deduplication>()
        val toDelete = profiles.mapNotNull {
            val bean = it.requireBean()
            val deduplication = Deduplication(bean, bean.javaClass.name)
            if (uniqueProxies.add(deduplication)) {
                null
            } else {
                it
            }
        }
        if (toDelete.isEmpty()) return@launch

        val ids = toDelete.map { it.id }
        uiState.update { state ->
            state.copy(
                alertForDelete = AlertForDelete(
                    size = toDelete.size,
                    summary = nameSummary(toDelete),
                    confirm = {
                        dismissAlert()
                        runOnIoDispatcher {
                            ProfileManager.deleteProfiles(groupId, ids)
                        }
                    },
                ),
            )
        }
    }

    fun dismissAlert() {
        uiState.update { it.copy(alertForDelete = null) }
    }

    private fun nameSummary(profiles: List<ProxyEntity>): String {
        return profiles.joinToString(separator = "\n") { it.displayName() }
    }

    fun importFile(
        file: PlatformFile,
        onProxiesFound: (List<AbstractBean>) -> Unit,
        onSubscriptionFound: (String) -> Unit,
        onNoProxies: () -> Unit,
        onError: (String) -> Unit,
    ) = runOnIoDispatcher {
        try {
            val fileName = file.name
            val bytes = file.readBytes()
            val proxies = mutableListOf<AbstractBean>()
            if (fileName.endsWith(".zip")) {
                ZipInputStream(bytes.inputStream()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val fileText = zip.bufferedReader().readText()
                        RawUpdater.parseRaw(fileText, entry.name)?.let { beans ->
                            proxies.addAll(beans)
                        }
                        zip.closeEntry()
                    }
                }
            } else {
                val fileText = bytes.decodeToString()
                RawUpdater.parseRaw(fileText, fileName)?.let { beans ->
                    proxies.addAll(beans)
                }
            }
            if (proxies.isEmpty()) {
                onNoProxies()
            } else {
                onProxiesFound(proxies)
            }

        } catch (e: SubscriptionFoundException) {
            onSubscriptionFound(e.link)
        } catch (e: Exception) {
            Logs.w(e)
            onError(e.readableMessage)
        }
    }

}
