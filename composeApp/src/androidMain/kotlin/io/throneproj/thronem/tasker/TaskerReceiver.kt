/******************************************************************************Add commentMore actions
 * Copyright (C) 2022 by nekohasekai <contact-git@sekai.icu>                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package io.throneproj.thronem.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.repository.resolveRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class TaskerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskerBundle.ACTION_FIRE_SETTING) return
        if (!DataStore.enableTasker.getBlocking()) return

        val settings = TaskerBundle.fromIntent(intent)
        when (settings.action) {
            TaskerBundle.ACTION_START -> {
                var reload = false
                if (settings.profileId > 0 && DataStore.selectedProxySet.getBlocking() != settings.profileId) {
                    val setExists = runBlocking {
                        ThroneDatabase.proxySetDao.getById(settings.profileId).firstOrNull() != null
                    }
                    if (setExists) {
                        DataStore.selectedProxySet.setBlocking(settings.profileId)
                        reload = DataStore.currentProfile.getBlocking() != 0L
                    }
                }
                if (reload) {
                    resolveRepository().reloadService()
                } else {
                    resolveRepository().startService()
                }
            }

            TaskerBundle.ACTION_STOP -> {
                resolveRepository().stopService()
            }
        }
    }
}