package io.throneproj.thronem.ui

import io.throneproj.thronem.GroupType
import io.throneproj.thronem.SubscriptionType
import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.GroupManager
import io.throneproj.thronem.database.ProfileManager
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.SubscriptionBean
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.BeanConverters
import io.throneproj.thronem.group.GroupUpdater
import io.throneproj.thronem.ktx.b64Decode
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.defaultOr
import io.throneproj.thronem.ktx.parseProxies
import io.throneproj.thronem.ktx.zlibDecompress
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

sealed interface ImportLinkPreview {
    object Ignore : ImportLinkPreview
    class Subscription(val group: ProxyGroup) : ImportLinkPreview
    class Profiles(val proxies: List<AbstractBean>) : ImportLinkPreview
}

fun isSubscriptionUri(uri: String): Boolean {
    return uri.startsWith("sing-box://import-remote-profile?") ||
            uri.startsWith("thronem://subscription?")
}

class ImportLinkInteractor {

    suspend fun parseUri(uri: String): ImportLinkPreview {
        return if (isSubscriptionUri(uri)) {
            val group = parseSubscription(uri)
            if (group == null) ImportLinkPreview.Ignore else ImportLinkPreview.Subscription(group)
        } else {
            ImportLinkPreview.Profiles(parseProfiles(uri))
        }
    }

    fun parseSubscription(uri: String): ProxyGroup? {
        if (
            uri.startsWith("thronem://") && !uri.startsWith("thronem://subscription?") ||
            uri.startsWith("sing-box://") && !uri.startsWith("sing-box://import-remote-profile?")
        ) {
            return null
        }

        val urlForQuery = parseURL(uri)
        val group: ProxyGroup
        val url = defaultOr(
            "",
            { urlForQuery.queryParameter("url") },
            {
                when (urlForQuery.scheme) {
                    "http", "https" -> uri
                    else -> null
                }
            },
        )
        if (url.isNotBlank()) {
            group = ProxyGroup(type = GroupType.SUBSCRIPTION)
            group.subscription = SubscriptionBean().apply {
                // cleartext format
                link = url
                type = when (urlForQuery.queryParameter("type").lowercase()) {
                    "oocv1" -> SubscriptionType.OOCv1
                    "sip008" -> SubscriptionType.SIP008
                    else -> SubscriptionType.RAW
                }
            }

            group.name = defaultOr(
                "",
                { urlForQuery.queryParameter("name") },
                { urlForQuery.fragment },
            )
        } else {
            val data =
                uri.substringAfter('?', "").substringBefore('#').blankAsNull() ?: return null
            group = BeanConverters.deserialize(
                ProxyGroup().apply { export = true },
                data.b64Decode().zlibDecompress(),
            ).apply {
                export = false
            }
        }

        if (group.name.isNullOrBlank() && group.subscription?.link.isNullOrBlank() && group.subscription?.token.isNullOrBlank()) {
            return null
        }
        group.name = group.name.blankAsNull() ?: ("Subscription #" + System.currentTimeMillis())
        return group
    }

    suspend fun parseProfiles(uri: String): List<AbstractBean> {
        return parseProxies(uri)
    }

    suspend fun createSubscriptionGroup(group: ProxyGroup): ProxyGroup {
        return GroupManager.createGroup(group)
    }

    suspend fun importSubscription(group: ProxyGroup) {
        val createdGroup = createSubscriptionGroup(group)
        GroupUpdater.executeUpdate(createdGroup, true)
    }

    suspend fun importProfiles(proxies: List<AbstractBean>): Int {
        val targetId = DataStore.selectedGroupForImport()
        for (proxy in proxies) {
            ProfileManager.createProfile(targetId, proxy)
        }
        DataStore.selectedGroup.set(targetId)
        return proxies.size
    }
}
