package io.throneproj.thronem.di

import io.throneproj.thronem.bg.ServiceEventMirror
import io.throneproj.thronem.compose.material3.PlatformMaterialApi
import io.throneproj.thronem.compose.material3.TvPlatformMaterialApi
import io.throneproj.thronem.compose.material3.standardPlatformMaterialApi
import io.throneproj.thronem.compose.theme.PlatformThemeApi
import io.throneproj.thronem.compose.theme.TvPlatformThemeApi
import io.throneproj.thronem.compose.theme.standardPlatformThemeApi
import io.throneproj.thronem.repository.AndroidRepository
import io.throneproj.thronem.repository.Repository
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformMaterialApi(): PlatformMaterialApi {
    return if (resolveRepository().isTv) {
        TvPlatformMaterialApi
    } else {
        standardPlatformMaterialApi()
    }
}

internal actual fun platformThemeApi(): PlatformThemeApi {
    return if (resolveRepository().isTv) {
        TvPlatformThemeApi
    } else {
        standardPlatformThemeApi()
    }
}

internal actual fun platformRepositoryModule(repository: Repository): Module = module {
    val androidRepository = repository as? AndroidRepository
        ?: error("Android platform requires AndroidRepository, got ${repository::class.qualifiedName}")
    single<AndroidRepository> { androidRepository }
    single<Repository> { get<AndroidRepository>() }
    if (repository.isMainProcess) {
        single {
            ServiceEventMirror(
                coreClient = get(),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            )
        }
    }
}

internal actual fun platformKoinModules(): List<Module> = listOf(androidNavigationModule)
