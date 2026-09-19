/******************************************************************************
 *                                                                            *
 * Copyright (C) 2021 by nekohasekai <contact-sagernet@sekai.icu>             *
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

package io.throneproj.thronem.group

import io.throneproj.thronem.database.DataStore
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.database.SubscriptionBean
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.shadowsocks.ShadowsocksBean
import io.throneproj.thronem.fmt.shadowsocks.pluginToLocal
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.addPathSegments
import io.throneproj.thronem.ktx.applyDefaultValues
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.generateUserAgent
import io.throneproj.thronem.ktx.kxs
import io.throneproj.thronem.ktx.URL
import io.throneproj.thronem.core.resolveHttpClientFactory
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.Res
import io.throneproj.thronem.resources.ooc_missing_protocol
import io.throneproj.thronem.resources.ooc_subscription_token_invalid
import kotlinx.serialization.Serializable

/** https://github.com/Shadowsocks-NET/OpenOnlineConfig */
object OpenOnlineConfigUpdater : GroupUpdater() {

    const val OOC_VERSION = 1
    val OOC_PROTOCOLS = listOf("shadowsocks")

    @Serializable
    private data class OOCSubscriptionToken(
        val version: Int,
        val baseUrl: String,
        val secret: String,
        val userId: String,
        val certSha256: String? = null,
    )

    @Serializable
    private data class OOCResponse(
        val protocols: List<String> = emptyList(),
        val username: String? = null,
        val bytesUsed: Long? = null,
        val bytesRemaining: Long? = null,
        val expiryDate: Long? = null,
        val shadowsocks: List<OOCShadowsocksProfile> = emptyList(),
    )

    @Serializable
    private data class OOCShadowsocksProfile(
        val name: String? = null,
        val address: String? = null,
        val port: Int? = null,
        val method: String? = null,
        val password: String? = null,
        val pluginName: String? = null,
        val pluginOptions: String? = null,
    )

    override suspend fun doUpdate(
        proxyGroup: ProxyGroup,
        subscription: SubscriptionBean,
        byUser: Boolean,
        warnings: MutableList<GroupUpdateWarning>,
    ): GroupUpdateResult.Success {
        val repository = resolveRepository()
        val token: OOCSubscriptionToken
        val baseLink: URL
        val certSha256: String?
        val httpClientFactory = resolveHttpClientFactory()
        try {
            token = kxs.decodeFromString(subscription.token)
            val version = token.version
            if (version != OOC_VERSION) {
                error("Unsupported OOC version $version")
            }
            val baseUrl = token.baseUrl
            when {
                baseUrl.isBlank() -> {
                    error("Missing field: baseUrl")
                }

                baseUrl.endsWith("/") -> {
                    error("baseUrl must not contain a trailing slash")
                }

                !baseUrl.startsWith("https://") -> {
                    error("Protocol scheme must be https")
                }

                else -> baseLink = httpClientFactory.parseURL(baseUrl)
            }
            val secret = token.secret
            if (secret.isBlank()) error("Missing field: secret")
            baseLink.addPathSegments(secret, "ooc/v1")

            val userId = token.userId
            if (userId.isBlank()) error("Missing field: userId")
            baseLink.addPathSegments(userId)
            certSha256 = token.certSha256?.blankAsNull()
        } catch (e: Exception) {
            Logs.e("OOC token check failed, token = ${subscription.token}", e)
            error(repository.getString(Res.string.ooc_subscription_token_invalid))
        }

        val response = httpClientFactory.newHttpClient().apply {
            if (DataStore.serviceState.connected) {
                trySocks5(
                    DataStore.mixedPort.get(),
                )
            }
            // Strict !!!
            restrictedTLS()
            certSha256?.let {
                pinnedSHA256(it)
            }
        }.newRequest().apply {
            setURL(baseLink.string)
            setUserAgent(generateUserAgent(subscription.customUserAgent))
        }.execute()

        val oocResponse: OOCResponse = try {
            kxs.decodeFromString(response.content.value)
        } catch (e: Exception) {
            Logs.e("OOC response parse failed", e)
            error(repository.getString(Res.string.ooc_subscription_token_invalid))
        }

        val protocols = oocResponse.protocols
        for (protocol in protocols) {
            if (protocol !in OOC_PROTOCOLS) {
                warnings += GroupUpdateWarning(
                    proxyGroup.displayName(),
                    repository.getString(Res.string.ooc_missing_protocol, protocol),
                )
            }
        }

        subscription.username = oocResponse.username.orEmpty()
        subscription.bytesUsed = oocResponse.bytesUsed ?: -1
        subscription.bytesRemaining = oocResponse.bytesRemaining ?: -1
        subscription.expiryDate = oocResponse.expiryDate ?: -1
        subscription.applyDefaultValues()

        val proxies = mutableListOf<AbstractBean>()

        for (protocol in protocols) {
            when (protocol) {
                "shadowsocks" -> for (profile in oocResponse.shadowsocks) {
                    val bean = ShadowsocksBean()

                    bean.name = profile.name.orEmpty()
                    bean.serverAddress = profile.address.orEmpty()
                    bean.serverPort = profile.port ?: 8388
                    bean.method = profile.method.orEmpty()
                    bean.password = profile.password.orEmpty()

                    // check plugin exists?
                    // check pluginVersion?
                    // TODO support pluginArguments
                    val pluginName = profile.pluginName
                    if (!pluginName.isNullOrBlank()) {
                        bean.plugin = pluginName + ";" + profile.pluginOptions.orEmpty()
                    }

                    proxies.add(bean.applyDefaultValues().apply { pluginToLocal() })
                }
            }
        }

        return tidyProxies(proxies, subscription, proxyGroup, byUser, warnings)
    }
}
