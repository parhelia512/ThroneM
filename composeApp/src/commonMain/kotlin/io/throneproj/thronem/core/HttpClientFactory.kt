package io.throneproj.thronem.core

import io.nekohasekai.libbox.HTTPClient
import io.nekohasekai.libbox.Libbox
import io.throneproj.thronem.ktx.USER_AGENT
import io.throneproj.thronem.ktx.URL
import org.koin.core.context.GlobalContext

interface HttpClientFactory {
    fun newHttpClient(): HTTPClient
    fun parseURL(urlString: String): URL
    val userAgent: String
}

internal object LibboxHttpClientFactory : HttpClientFactory {
    override fun newHttpClient(): HTTPClient = Libbox.newHTTPClient()

    override fun parseURL(urlString: String): URL =
        io.throneproj.thronem.ktx.parseURL(urlString)

    override val userAgent: String
        get() = USER_AGENT
}

internal fun resolveHttpClientFactory(): HttpClientFactory {
    return GlobalContext.getOrNull()?.get() ?: LibboxHttpClientFactory
}
