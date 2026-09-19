package io.throneproj.thronem.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.throneproj.thronem.compose.theme.AppTheme
import io.throneproj.thronem.database.preference.createSimpleConfigurationDataStore
import io.throneproj.thronem.di.initThroneMKoin
import io.throneproj.thronem.repository.Repository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.io.path.createTempDirectory

@Suppress("NewApi")
internal fun createPreviewRoot(): File = createTempDirectory("thronem-preview").toFile()

internal fun previewConfigurationDataStore(
    root: File,
    scope: CoroutineScope,
): DataStore<Preferences> = createSimpleConfigurationDataStore(
    root.resolve("configuration.preferences_pb"),
    scope,
)

@Composable
internal expect fun previewRepository(): Repository

private fun preparePreviewKoin(repository: Repository): Koin {
    GlobalContext.getOrNull()?.let { koin ->
        if (koin.getOrNull<Repository>() != null) return koin
        // A previous preview registered the context and then failed while loading
        // modules, leaving an empty graph behind. Nothing can resolve from it.
        stopKoin()
    }
    initThroneMKoin(repository)
    return GlobalContext.get()
}

@Composable
internal fun PreviewContainer(content: @Composable () -> Unit) {
    val repository = previewRepository()
    remember(repository) { preparePreviewKoin(repository) }
    AppTheme(content)
}
