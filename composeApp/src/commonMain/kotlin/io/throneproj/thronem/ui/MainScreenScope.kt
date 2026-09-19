package io.throneproj.thronem.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import org.koin.core.Koin
import org.koin.core.scope.Scope

object MainScreenScope

internal class MainScreenScopeHolder(koin: Koin) : ViewModel() {

    val scope: Scope = koin.createScope<MainScreenScope>()

    // Held by the ViewModel so it survives an Activity recreate (e.g. toggling
    // dark mode). The Navigator is cached in `scope`, which also survives
    // recreate; if this list were rebuilt by the composable instead, the
    // Navigator would keep pointing at the old, detached list while NavDisplay
    // rendered the new one — and the back button would stop working.
    // The Dashboard entry hosts both the proxy sets picker (service down) and
    // the live dashboard (service up), so it is always the start page.
    val backStack: MutableList<NavKey> = mutableStateListOf(
        NavRoutes.Dashboard,
    )

    override fun onCleared() {
        scope.close()
        super.onCleared()
    }
}
