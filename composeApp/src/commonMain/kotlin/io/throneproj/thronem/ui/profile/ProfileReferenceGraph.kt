package io.throneproj.thronem.ui.profile

import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ThroneDatabase
import io.throneproj.thronem.fmt.internal.ChainBean

private suspend fun ProxyEntity.directProfileReferences(): List<ProxyEntity> =
    when (val bean = requireBean()) {
        is ChainBean -> ThroneDatabase.proxyDao.getEntities(bean.proxies)

        else -> emptyList()
    }

private suspend fun collectProfileReferenceIds(
    roots: Iterable<ProxyEntity>,
): Set<Long> {
    val references = LinkedHashSet<Long>()

    suspend fun visit(entity: ProxyEntity) {
        if (!references.add(entity.id)) return
        for (referencedProfile in entity.directProfileReferences()) {
            visit(referencedProfile)
        }
    }

    for (root in roots) {
        visit(root)
    }
    return references
}

internal suspend fun ProxyEntity.containsProfileReference(targetId: Long): Boolean {
    return targetId in collectProfileReferenceIds(listOf(this))
}
