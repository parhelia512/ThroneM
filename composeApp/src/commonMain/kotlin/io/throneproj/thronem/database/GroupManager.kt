package io.throneproj.thronem.database

import io.throneproj.thronem.GroupType
import io.throneproj.thronem.bg.SubscriptionUpdater
import io.throneproj.thronem.ktx.applyDefaultValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object GroupManager {

    suspend fun clearGroup(groupId: Long) {
        ThroneDatabase.proxyDao.deleteAll(groupId)
    }

    suspend fun rearrange(groupId: Long) {
        val entities = withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.getByGroup(groupId).first()
        }
        for (index in entities.indices) {
            entities[index].userOrder = (index + 1).toLong()
        }
        withContext(Dispatchers.IO) {
            ThroneDatabase.proxyDao.updateProxy(entities)
        }
    }

    suspend fun createGroup(group: ProxyGroup): ProxyGroup {
        group.userOrder = ThroneDatabase.groupDao.nextOrder() ?: 1
        group.id = ThroneDatabase.groupDao.createGroup(group.applyDefaultValues())
        if (group.type == GroupType.SUBSCRIPTION) {
            SubscriptionUpdater.reconfigureUpdater()
        }
        return group
    }

    suspend fun updateGroup(group: ProxyGroup) {
        ThroneDatabase.groupDao.updateGroup(group)
        if (group.type == GroupType.SUBSCRIPTION) {
            SubscriptionUpdater.reconfigureUpdater()
        }
    }

    suspend fun deleteGroup(groupId: Long) {
        ThroneDatabase.groupDao.deleteById(groupId)
        ThroneDatabase.proxyDao.deleteByGroup(groupId)
        SubscriptionUpdater.reconfigureUpdater()
    }

    suspend fun deleteGroup(group: List<Long>) {
        ThroneDatabase.groupDao.deleteByIds(group)
        ThroneDatabase.proxyDao.deleteByGroup(group.toLongArray())
        SubscriptionUpdater.reconfigureUpdater()
    }

}
