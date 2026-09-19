package io.throneproj.thronem.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.throneproj.thronem.bg.RouteAssetUpdater
import io.throneproj.thronem.bg.routeGeoDir
import io.throneproj.thronem.database.AssetEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.runOnIoDispatcher
import io.throneproj.thronem.platform.PathLimits
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.duplicate_name
import io.throneproj.thronem.resources.expect_srs
import io.throneproj.thronem.resources.filename_too_long_bytes
import io.throneproj.thronem.resources.filename_too_long_characters
import io.throneproj.thronem.resources.invalid_filename
import io.throneproj.thronem.resources.path_too_long_bytes
import io.throneproj.thronem.resources.path_too_long_characters
import io.throneproj.thronem.resources.warn_starte_with_geo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

@Immutable
internal data class AssetEditUiState(
    val name: String = "",
    val link: String = "",
    val autoUpdateDelay: Int = 0,
)

@Stable
internal class AssetEditViewModel(
    assetName: String,
) : ViewModel() {

    val uiState: StateFlow<AssetEditUiState>
        field = MutableStateFlow(AssetEditUiState())

    private val initialState = MutableStateFlow<AssetEditUiState?>(null)
    val isDirty = combine(uiState, initialState) { currentState, initialState ->
        initialState?.let {
            it != currentState
        } ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    var editingName: String = ""
    var isNew = false

    init {
        viewModelScope.launch {
            initialize(assetName)
        }
    }

    suspend fun initialize(name: String) {
        isNew = false
        shouldUpdateFromInternet = false
        initialState.value = null
        val asset = ThroneDatabase.assetDao.get(name) ?: AssetEntity().also {
            isNew = true
        }
        editingName = name
        uiState.update { state ->
            state.copy(
                name = asset.name,
                link = asset.url,
                autoUpdateDelay = asset.autoUpdateDelay,
            ).also {
                initialState.value = it
            }
        }
    }

    var shouldUpdateFromInternet = false

    fun save() = runOnIoDispatcher {
        if (isNew) {
            val entity = AssetEntity()
            entity.loadFromUiState(uiState.value)
            ThroneDatabase.assetDao.create(entity)
        } else if (isDirty.value) {
            val entity = ThroneDatabase.assetDao.get(editingName) ?: return@runOnIoDispatcher
            entity.loadFromUiState(uiState.value)
            ThroneDatabase.assetDao.update(entity)
        }
        RouteAssetUpdater.reconfigureUpdater()
    }

    private fun AssetEntity.loadFromUiState(state: AssetEditUiState) {
        name = state.name
        url = state.link
        autoUpdateDelay = state.autoUpdateDelay
    }

    fun setName(name: String) = viewModelScope.launch {
        uiState.update {
            it.copy(name = name)
        }
    }

    fun setLink(link: String) = viewModelScope.launch {
        uiState.update {
            val name = it.name.blankAsNull() ?: link.substringAfterLast("/")
            it.copy(
                name = name,
                link = link,
            )
        }
        shouldUpdateFromInternet = true
    }

    fun setAutoUpdateDelay(autoUpdateDelay: Int) = viewModelScope.launch {
        uiState.update {
            it.copy(autoUpdateDelay = autoUpdateDelay)
        }
    }

    suspend fun validate(text: String): StringOrRes? {
        val limits = PathLimits.current
        if (!limits.acceptsName(text)) {
            return limits.tooLongMessage(
                inBytes = Res.string.filename_too_long_bytes,
                inCharacters = Res.string.filename_too_long_characters,
                limit = limits.maxNameLength,
                text = text,
            )
        }
        val file = routeGeoDir(resolveRepository().externalAssetsDir).resolve(text)
        if (file.canonicalFile.name != text) {
            return StringOrRes.Res(Res.string.invalid_filename)
        }
        if (!limits.acceptsPath(file.absolutePath)) {
            return limits.tooLongMessage(
                inBytes = Res.string.path_too_long_bytes,
                inCharacters = Res.string.path_too_long_characters,
                limit = limits.maxPathLength,
                text = file.absolutePath,
            )
        }
        if (text != editingName && ThroneDatabase.assetDao.get(text) != null) {
            return StringOrRes.Res(Res.string.duplicate_name)
        }
        if (!text.endsWith(SingBoxOptions.RULE_SET_FILE_SUFFIX)) {
            return StringOrRes.Res(Res.string.expect_srs)
        }
        if (text.startsWith("geosite-") || text.startsWith("geoip-")) {
            return StringOrRes.Res(Res.string.warn_starte_with_geo)
        }
        return null
    }

    fun PathLimits.tooLongMessage(
        inBytes: StringResource,
        inCharacters: StringResource,
        limit: Int,
        text: String,
    ): StringOrRes = StringOrRes.ResWithParams(
        if (countsUtf8Bytes) {
            inBytes
        } else {
            inCharacters
        },
        limit,
        lengthOf(text),
    )

}
