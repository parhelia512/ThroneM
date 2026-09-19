package io.throneproj.thronem.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.throneproj.thronem.RuleProvider
import io.throneproj.thronem.bg.NoUpdateException
import io.throneproj.thronem.bg.RouteAssetUpdater
import io.throneproj.thronem.bg.currentEpochSeconds
import io.throneproj.thronem.bg.routeAssetVersionFile
import io.throneproj.thronem.bg.updateManagedRouteAssets
import io.throneproj.thronem.bg.updateSingleRouteAsset
import io.throneproj.thronem.database.AssetEntity
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.ktx.runOnDefaultDispatcher
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.route_asset_no_update
import io.throneproj.thronem.utils.copyBundledRuleSetAssetsIfNeeded
import io.throneproj.thronem.utils.extractBundledGeoAssets
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Immutable
internal data class AssetsUiState(
    val process: Float? = null,
    val assets: List<AssetItem> = emptyList(),
    val pendingDeleteCount: Int = 0,
)

@Immutable
internal data class AssetItem(
    val file: File,
    val version: String,
    val builtIn: Boolean,
    val autoUpdateDelay: Int = 0,
    val isUpdating: Boolean = false,
)

@Immutable
internal sealed interface AssetsScreenUiEvent {
    class Snackbar(val message: StringOrRes) : AssetsScreenUiEvent
}

@Stable
internal class AssetsScreenViewModel(
    assetsDir: File,
    geoDir: File,
) : ViewModel() {

    companion object {
        fun isBuiltIn(index: Int): Boolean = index < 2
    }

    val uiState: StateFlow<AssetsUiState>
        field = MutableStateFlow(AssetsUiState())

    val uiEvent: SharedFlow<AssetsScreenUiEvent>
        field = MutableSharedFlow<AssetsScreenUiEvent>()

    private lateinit var assetsDir: File
    private lateinit var geoDir: File

    private val firstDownloadStarted = mutableSetOf<String>()
    private var initializedFor: Pair<String, String>? = null
    private var assetsObserveJob: Job? = null

    private var deleteTimer: Job? = null
    private val hiddenAssetsAccess = Mutex()
    private val hiddenAssets = mutableSetOf<String>()

    init {
        initialize(assetsDir, geoDir)
    }

    fun initialize(assetsDir: File, geoDir: File) {
        val args = assetsDir.absolutePath to geoDir.absolutePath
        if (initializedFor == args && assetsObserveJob?.isActive == true) return
        initializedFor = args
        assetsObserveJob?.cancel()
        firstDownloadStarted.clear()
        this.assetsDir = assetsDir
        this.geoDir = geoDir

        assetsObserveJob = viewModelScope.launch {
            ThroneDatabase.assetDao.getAll().collectLatest { assets ->
                for (asset in assets) {
                    if (needsFirstDownload(asset) && firstDownloadStarted.add(asset.name)) {
                        updateSingleAsset(geoDir.resolve(asset.name))
                    }
                }
                refreshAssets0(assets)
            }
        }
    }

    private fun needsFirstDownload(asset: AssetEntity): Boolean {
        return asset.lastUpdated == 0L && !geoDir.resolve(asset.name).isFile
    }

    fun refreshAssets() = viewModelScope.launch {
        val assets = ThroneDatabase.assetDao.getAll().first()
        refreshAssets0(assets)
    }

    private suspend fun refreshAssets0(dbAssets: List<AssetEntity>) {
        val assetsByName = dbAssets.associateBy(AssetEntity::name)
        val files = buildList {
            add(assetsDir.resolve("geoip.version.txt"))
            add(assetsDir.resolve("geosite.version.txt"))
            dbAssets.forEach { add(geoDir.resolve(it.name)) }
        }

        hiddenAssetsAccess.withLock {
            uiState.update { state ->
                state.copy(
                    assets = files.mapIndexed { index, file ->
                        buildAssetItem(index, file, assetsByName[file.name])
                    }.filterNot { hiddenAssets.contains(it.file.name) },
                    pendingDeleteCount = hiddenAssets.size,
                    process = null,
                )
            }
        }
    }

    private fun buildAssetItem(index: Int, file: File, entity: AssetEntity?): AssetItem {
        val builtIn = isBuiltIn(index)
        val version = if (builtIn) {
            file.takeIf(File::isFile)
                ?.readText()
                ?.trim()
                .blankAsNull()
                ?: "Unknown"
        } else {
            entity?.version.blankAsNull()
                ?: routeAssetVersionFile(assetsDir, file.name).takeIf(File::isFile)
                    ?.readText()
                    ?.trim()
                    .blankAsNull()
                ?: "Unknown"
        }
        return AssetItem(
            file = file,
            version = version,
            builtIn = builtIn,
            autoUpdateDelay = entity?.autoUpdateDelay ?: 0,
            isUpdating = false,
        )
    }

    suspend fun deleteAssets(files: List<File>) {
        for (file in files) {
            file.delete()
            val versionFile = routeAssetVersionFile(assetsDir, file.name)
            if (versionFile.isFile) versionFile.delete()
            ThroneDatabase.assetDao.delete(file.name)
        }
        RouteAssetUpdater.reconfigureUpdater()
    }

    fun updateAsset(cacheDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateAsset0(cacheDir)
            } catch (_: NoUpdateException) {
                uiEvent.emit(AssetsScreenUiEvent.Snackbar(StringOrRes.Res(Res.string.route_asset_no_update)))
            } catch (e: Exception) {
                Logs.e(e)
                uiEvent.emit(AssetsScreenUiEvent.Snackbar(StringOrRes.Direct(e.readableMessage)))
            }
            RouteAssetUpdater.reconfigureUpdater()
            val assets = ThroneDatabase.assetDao.getAll().first()
            refreshAssets0(assets)
        }
    }

    private suspend fun updateAsset0(cacheDir: File) {
        uiState.update { it.copy(process = 0f) }

        var process = 0f
        updateManagedRouteAssets(
            externalAssetsDir = assetsDir,
            cacheDir = cacheDir,
        ) { progressDelta ->
            process += progressDelta
            uiState.update { it.copy(process = process) }
        }
    }

    fun resetRuleSet() = viewModelScope.launch(Dispatchers.IO) {
        if (DataStore.rulesProvider.get() != RuleProvider.OFFICIAL) return@launch
        uiState.update { it.copy(process = 0f) }
        try {
            copyBundledRuleSetAssetsIfNeeded()
            assetsDir.resolve("geoip.version.txt").delete()
            assetsDir.resolve("geosite.version.txt").delete()
            extractBundledGeoAssets(
                bundledDir = resolveRepository().filesDir.resolve("sing-box"),
                workingDir = resolveRepository().filesDir,
            )
            DataStore.routeAssetsLastUpdated.set(currentEpochSeconds())
            RouteAssetUpdater.reconfigureUpdater()
        } catch (e: Exception) {
            Logs.e(e)
            uiEvent.emit(AssetsScreenUiEvent.Snackbar(StringOrRes.Direct(e.readableMessage)))
        }
        val assets = ThroneDatabase.assetDao.getAll().first()
        refreshAssets0(assets)
    }

    fun updateSingleAsset(asset: File) = viewModelScope.launch(Dispatchers.IO) {
        try {
            updateSingleAsset0(asset)
        } catch (e: Exception) {
            Logs.e(e)
            uiEvent.emit(AssetsScreenUiEvent.Snackbar(StringOrRes.Direct(e.readableMessage)))
        }
        RouteAssetUpdater.reconfigureUpdater()
        val assets = ThroneDatabase.assetDao.getAll().first()
        refreshAssets0(assets)
    }

    private suspend fun updateSingleAsset0(asset: File) {
        val entity = ThroneDatabase.assetDao.get(asset.name) ?: return

        setUpdating(asset, isUpdating = true)
        try {
            entity.version = updateSingleRouteAsset(entity, assetsDir)
        } finally {
            setUpdating(asset, isUpdating = false)
        }
        entity.lastUpdated = currentEpochSeconds()
        ThroneDatabase.assetDao.update(entity)
    }

    private fun setUpdating(asset: File, isUpdating: Boolean) {
        uiState.update { state ->
            state.copy(
                assets = state.assets.map {
                    if (it.file == asset) {
                        it.copy(isUpdating = isUpdating)
                    } else {
                        it
                    }
                },
            )
        }
    }

    fun undoableRemove(fileName: String) = viewModelScope.launch {
        hiddenAssetsAccess.withLock {
            uiState.update { state ->
                val assets = state.assets.toMutableList()
                val assetIndex = assets.indexOfFirst { it.file.name == fileName }
                if (assetIndex >= 0) {
                    val asset = assets.removeAt(assetIndex)
                    hiddenAssets.add(asset.file.name)
                }
                state.copy(
                    assets = assets,
                    pendingDeleteCount = hiddenAssets.size,
                )
            }
        }
        startDeleteTimer()
    }

    private fun startDeleteTimer() {
        deleteTimer?.cancel()
        deleteTimer = viewModelScope.launch {
            delay(5000.milliseconds)
            commit()
        }
    }

    fun undo() = viewModelScope.launch {
        deleteTimer?.cancel()
        deleteTimer = null
        hiddenAssetsAccess.withLock {
            hiddenAssets.clear()
        }
        refreshAssets()
    }

    fun commit() = runOnDefaultDispatcher {
        deleteTimer?.cancel()
        deleteTimer = null
        val toDelete = hiddenAssetsAccess.withLock {
            val pending = hiddenAssets.toList()
            hiddenAssets.clear()
            pending
        }
        withContext(Dispatchers.IO) {
            for (fileName in toDelete) {
                val file = if (fileName.endsWith(".version.txt")) {
                    assetsDir.resolve(fileName)
                } else {
                    geoDir.resolve(fileName)
                }
                file.delete()
                if (!fileName.endsWith(".version.txt")) {
                    val versionFile = routeAssetVersionFile(assetsDir, fileName)
                    if (versionFile.isFile) versionFile.delete()
                    ThroneDatabase.assetDao.delete(fileName)
                }
            }
            RouteAssetUpdater.reconfigureUpdater()
        }
    }
}
