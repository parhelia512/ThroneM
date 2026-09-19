package io.throneproj.thronem.core

import io.throneproj.thronem.ktx.Logs
import java.io.IOException
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL

class SubscriptionHttpResponse(
    val body: String,
    private val headers: Map<String, List<String>>,
) {
    /** Case-insensitive header lookup; empty string when absent. */
    fun header(name: String): String {
        for ((key, values) in headers) {
            if (key.equals(name, ignoreCase = true)) {
                return values.firstOrNull().orEmpty()
            }
        }
        return ""
    }
}

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 30_000

/**
 * Credentials handed to the process-wide [Authenticator] by the next request.
 * Android's java.net.Authenticator has no getDefault(), so the authenticator
 * is installed once and reads the current credentials from here.
 */
@Volatile
private var socksCredentials: Pair<String, String>? = null

@Volatile
private var authenticatorInstalled = false

private fun installAuthenticatorOnce() {
    if (authenticatorInstalled) return
    synchronized(SubscriptionHttpResponse::class.java) {
        if (authenticatorInstalled) return
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                val (user, password) = socksCredentials ?: return null
                return PasswordAuthentication(user, password.toCharArray())
            }
        })
        authenticatorInstalled = true
    }
}

/**
 * Minimal HTTP GET for subscription payloads. libbox's HTTPClient cannot
 * expose response headers (Subscription-Userinfo) nor SOCKS credentials, so
 * raw subscriptions go through java.net instead, which supports both.
 */
fun fetchSubscriptionResponse(
    url: String,
    userAgent: String,
    socks5Port: Int? = null,
    socks5Username: String? = null,
    socks5Password: String? = null,
): SubscriptionHttpResponse {
    val proxy = when {
        socks5Port == null -> Proxy.NO_PROXY
        else -> Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socks5Port))
    }

    val needsAuth = socks5Port != null &&
        !socks5Username.isNullOrBlank() &&
        !socks5Password.isNullOrBlank()
    if (needsAuth) {
        socksCredentials = socks5Username to socks5Password
        installAuthenticatorOnce()
    }
    try {
        val connection = URL(url).openConnection(proxy) as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", userAgent)
        val code = connection.responseCode
        if (code !in 200..299) {
            throw IOException("HTTP $code for $url")
        }
        val body = connection.inputStream.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
        val headers = connection.headerFields.orEmpty()
            .mapValues { entry -> entry.value.orEmpty() }
        return SubscriptionHttpResponse(body, headers)
    } catch (e: Exception) {
        if (e is IOException) throw e
        Logs.w("subscription fetch failed", e)
        throw IOException(e.message, e)
    }
}
