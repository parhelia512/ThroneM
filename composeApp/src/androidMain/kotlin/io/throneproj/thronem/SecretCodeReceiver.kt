package io.throneproj.thronem

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.ktx.runOnDefaultDispatcher
import io.throneproj.thronem.ktx.showToast
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.launcher_icon_restored

class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.data?.host != LauncherIcon.SECRET_CODE) return
        if (!LauncherIcon.hidden) return

        val pendingResult = goAsync()
        runOnDefaultDispatcher {
            try {
                LauncherIcon.hidden = false
                DataStore.hideLauncherIcon.set(false)
                showToast(resolveRepository().getString(Res.string.launcher_icon_restored), true)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
