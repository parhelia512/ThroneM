package io.throneproj.thronem.di

import io.throneproj.thronem.compose.material3.PlatformMaterialApi
import io.throneproj.thronem.compose.theme.PlatformThemeApi
import io.throneproj.thronem.core.CoreClient
import io.throneproj.thronem.core.LibboxCoreClient
import io.throneproj.thronem.core.HttpClientFactory
import io.throneproj.thronem.core.LibboxHttpClientFactory
import io.throneproj.thronem.repository.Repository
import io.throneproj.thronem.ui.ImportLinkInteractor
import io.throneproj.thronem.ui.openconnect.OpenConnectAuthController
import io.throneproj.thronem.ui.openvpn.OpenVPNAuthController
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private fun commonUiModule() = module {
    single<PlatformMaterialApi> { platformMaterialApi() }
    single<PlatformThemeApi> { platformThemeApi() }
    single<HttpClientFactory> { LibboxHttpClientFactory }
    single<CoreClient> { LibboxCoreClient.local() }
    singleOf(::ImportLinkInteractor)
    singleOf(::OpenConnectAuthController)
    single { OpenVPNAuthController(coreClient = get()) }
}

internal expect fun platformMaterialApi(): PlatformMaterialApi

internal expect fun platformThemeApi(): PlatformThemeApi

internal expect fun platformRepositoryModule(repository: Repository): Module

internal expect fun platformKoinModules(): List<Module>

fun initThroneMKoin(repository: Repository) {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(
            listOf(platformRepositoryModule(repository), commonUiModule(), commonNavigationModule) +
                platformKoinModules(),
        )
    }
}
