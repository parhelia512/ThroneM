package io.throneproj.thronem.warp

import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.getArray
import io.throneproj.thronem.ktx.getObject
import io.throneproj.thronem.ktx.getStr
import io.throneproj.thronem.ktx.toJsonMapKxs
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class WarpException(message: String) : Exception(message)

/**
 * Cloudflare WARP registration client, ported from LxBox's WarpClient. Same
 * flow as the official client / wgcf: an X25519 keypair is generated locally
 * and only the public key is registered via the Cloudflare API. Hosts are
 * tried in order; the first host that answers POST /reg wins for the
 * follow-up calls (device and token belong to it).
 */
class WarpClient(private val timeoutMs: Int = 5_000) {

    private var activeHost: String? = null

    private fun host(): String = activeHost ?: WarpEndpoints.API_HOSTS.first()

    /**
     * Registers a device. [licenseKey] (WARP+) is applied afterwards via a
     * PATCH that never fails the registration. [randomEndpoint] replaces the
     * default endpoint when [obfuscate] is on and the caller did not pin one.
     */
    fun register(
        nowIso8601: String,
        licenseKey: String? = null,
        endpoint: String = WarpEndpoints.DEFAULT_ENDPOINT,
        obfuscate: Boolean = false,
        randomEndpoint: String? = null,
    ): WarpAccount {
        val keyPair = generateWireGuardKeyPair()

        val body = buildString {
            append('{')
            append("\"key\":${jsonString(keyPair.publicKey)},")
            append("\"install_id\":\"\",")
            append("\"fcm_token\":\"\",")
            append("\"tos\":${jsonString(nowIso8601)},")
            append("\"model\":\"PC\",")
            append("\"type\":\"Android\",")
            append("\"locale\":\"en_US\"")
            append('}')
        }

        val response = postReg(body)
        if (response.statusCode != 200) {
            throw WarpException(
                "registration failed (HTTP ${response.statusCode}); API version may have changed",
            )
        }
        val json = try {
            response.body.toJsonMapKxs()
        } catch (_: Exception) {
            throw WarpException("bad response: not JSON")
        }

        val effectiveEndpoint = if (obfuscate &&
            endpoint == WarpEndpoints.DEFAULT_ENDPOINT &&
            !randomEndpoint.isNullOrBlank()
        ) {
            randomEndpoint
        } else {
            endpoint
        }

        var account = parseReg(json, privKey = keyPair.privateKey, endpoint = effectiveEndpoint)

        if (!licenseKey.isNullOrBlank()) {
            account = applyLicenseSafe(account, licenseKey.trim())
        }
        return account
    }

    private fun parseReg(json: JSONMap, privKey: String, endpoint: String): WarpAccount {
        val deviceId = json.getStr("id").orEmpty()
        val token = json.getStr("token").orEmpty()
        val accountId = json.getObject("account")?.getStr("id").orEmpty()

        val config = json.getObject("config")
            ?: throw WarpException("bad response: missing config")
        val clientId = config.getStr("client_id").orEmpty()

        val userPickedEndpoint = endpoint != WarpEndpoints.DEFAULT_ENDPOINT
        var peerPub = ""
        var host = endpoint
        val peer = (config.getArray("peers") as? List<JSONMap>)?.firstOrNull()
        if (peer != null) {
            peerPub = peer.getStr("public_key").orEmpty()
            if (!userPickedEndpoint) {
                val peerHost = peer.getObject("endpoint")?.getStr("host")
                if (!peerHost.isNullOrBlank()) host = peerHost
            }
        }
        if (peerPub.isBlank()) {
            throw WarpException("bad response: missing peer public_key")
        }

        val addresses = config.getObject("interface")?.getObject("addresses")
        val v4 = addresses?.getStr("v4").orEmpty()
        val v6 = addresses?.getStr("v6").orEmpty()
        if (v4.isBlank()) {
            throw WarpException("bad response: missing interface address")
        }

        return WarpAccount(
            privKey = privKey,
            peerPub = peerPub,
            clientV4 = v4,
            clientV6 = v6,
            clientId = clientId,
            accountId = accountId,
            deviceId = deviceId,
            token = token,
            endpoint = host,
            createdAt = System.currentTimeMillis().toString(),
        )
    }

    /**
     * Registers a MASQUE device, ported from LxBox's WarpClient.registerMasque.
     * Two steps (usque/mihomo):
     *   1. POST /reg with a real X25519 key → `id` + `token`.
     *   2. PATCH /reg/{id} (Bearer) with the ECDSA public key,
     *      `key_type=secp256r1`, `tunnel_type=masque` → server pubkey,
     *      interface addresses and the data-plane endpoint.
     *
     * The ECDSA private key is generated on device ([MasqueKeys]) and never
     * leaves it. [sni]/[idleTimeout]/[keepAlive] are client-side options; they
     * are stored with the account but do not affect registration.
     */
    fun registerMasque(
        nowIso8601: String,
        sni: String? = null,
        idleTimeout: String? = null,
        keepAlive: String? = null,
    ): MasqueAccount {
        val keys = MasqueKeys.generate()

        // Step 1 — plain POST /reg (mimics the Android app). The key is a real
        // X25519 public key (one-shot, discarded afterwards): the API checks
        // the point on the curve — random 32 bytes caught 401 "Invalid public
        // key" in LxBox §418.
        val regBody = buildString {
            append('{')
            append("\"key\":${jsonString(generateWireGuardKeyPair().publicKey)},")
            append("\"install_id\":\"\",")
            append("\"fcm_token\":\"\",")
            append("\"tos\":${jsonString(nowIso8601)},")
            append("\"model\":\"PC\",")
            append("\"type\":\"Android\",")
            append("\"locale\":\"en_US\"")
            append('}')
        }

        val regResponse = postReg(regBody)
        if (regResponse.statusCode != 200) {
            throw WarpException(
                "MASQUE registration failed (HTTP ${regResponse.statusCode}); " +
                    "API version may have changed",
            )
        }
        val regJson = try {
            regResponse.body.toJsonMapKxs()
        } catch (_: Exception) {
            throw WarpException("bad response: not JSON")
        }
        val deviceId = regJson.getStr("id").orEmpty()
        val token = regJson.getStr("token").orEmpty()
        if (deviceId.isEmpty() || token.isEmpty()) {
            throw WarpException("bad response: missing id/token for MASQUE enroll")
        }

        // Step 2 — PATCH enroll: swap the key for ECDSA, the type for masque.
        val patchBody = buildString {
            append('{')
            append("\"key\":${jsonString(keys.publicKeyDer)},")
            append("\"key_type\":\"secp256r1\",")
            append("\"tunnel_type\":\"masque\"")
            append('}')
        }
        val connection = try {
            openConnection(
                "${host()}/${WarpApi.VERSION}/reg/$deviceId",
                method = "PATCH",
                bearer = token,
            )
        } catch (e: Exception) {
            throw WarpException("network error (MASQUE enroll): $e")
        }
        val patchResponse = try {
            connection.outputStream.use {
                it.write(patchBody.toByteArray())
            }
            readResponse(connection)
        } catch (e: IOException) {
            throw WarpException("network error (MASQUE enroll): $e")
        }
        if (patchResponse.statusCode != 200) {
            throw WarpException(
                "MASQUE enroll failed (HTTP ${patchResponse.statusCode}); " +
                    "this API version may not support MASQUE",
            )
        }
        val patchJson = try {
            patchResponse.body.toJsonMapKxs()
        } catch (_: Exception) {
            throw WarpException("bad response: MASQUE enroll not JSON")
        }

        return parseMasqueEnroll(
            patchJson,
            privKeyDer = keys.privateKeyDer,
            deviceId = deviceId,
            token = token,
            createdAt = nowIso8601,
            sni = sni.orEmpty(),
            idleTimeout = idleTimeout.orEmpty(),
            keepAlive = keepAlive.orEmpty(),
        )
    }

    private fun parseMasqueEnroll(
        json: JSONMap,
        privKeyDer: String,
        deviceId: String,
        token: String,
        createdAt: String,
        sni: String,
        idleTimeout: String,
        keepAlive: String,
    ): MasqueAccount {
        val config = json.getObject("config")
            ?: throw WarpException("bad response: missing config (MASQUE)")

        // interface addresses (our ip/ipv6)
        val addresses = config.getObject("interface")?.getObject("addresses")
        val v4 = addresses?.getStr("v4").orEmpty()
        val v6 = addresses?.getStr("v6").orEmpty()
        if (v4.isBlank() && v6.isBlank()) {
            throw WarpException("bad response: missing interface address (MASQUE)")
        }

        // peers[0]: server pubkey + data-plane endpoint
        var serverPub = ""
        var server = MasqueAccount.DEFAULT_SERVER
        var port = MasqueAccount.DEFAULT_PORT
        val peer = (config.getArray("peers") as? List<JSONMap>)?.firstOrNull()
        if (peer != null) {
            serverPub = peer.getStr("public_key").orEmpty()
            val endpoint = peer.getObject("endpoint")
            if (endpoint != null) {
                val host = stripEndpointHost(endpoint.getStr("v4").orEmpty())
                if (host.isNotBlank()) server = host
                val ports = endpoint.getArray("ports")
                val firstPort = (ports as? List<*>)?.firstOrNull()
                if (firstPort is Number) {
                    port = firstPort.toInt()
                }
            }
        }
        if (serverPub.isBlank()) {
            throw WarpException("bad response: missing server public_key (MASQUE)")
        }

        return MasqueAccount(
            privKeyDer = privKeyDer,
            // CF sends the server pubkey as PEM → bare base64(DER) for the core.
            serverPubDer = MasqueKeys.normalizeServerPubKey(serverPub),
            clientV4 = v4,
            clientV6 = v6,
            server = server,
            port = if (port == 0) MasqueAccount.DEFAULT_PORT else port,
            deviceId = deviceId,
            token = token,
            createdAt = createdAt,
            sni = sni,
            idleTimeout = idleTimeout,
            keepAlive = keepAlive,
        )
    }

    /** Strips the `:0` stub from an endpoint (`162.159.198.1:0` → `162.159.198.1`). */
    private fun stripEndpointHost(raw: String): String {
        val i = raw.lastIndexOf(':')
        if (i <= 0) return raw
        return raw.substring(0, i)
    }

    /** PATCH account with the license; any failure keeps the free account. */
    private fun applyLicenseSafe(account: WarpAccount, license: String): WarpAccount {
        if (account.deviceId.isBlank() || account.token.isBlank()) {
            Logs.w("WARP: cannot apply license — no device id/token")
            return account.copy(license = license)
        }
        try {
            val connection = openConnection(
                "${host()}/${WarpApi.VERSION}/reg/${account.deviceId}/account",
                method = "PATCH",
                bearer = account.token,
            )
            connection.outputStream.use {
                it.write("{\"license\":${jsonString(license)}}".toByteArray())
            }
            val code = connection.responseCode
            if (code != 200) {
                Logs.w("WARP: license not applied (HTTP $code)")
                return account.copy(license = license)
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = body.toJsonMapKxs()
            val warpPlus = json["warp_plus"] == true ||
                json.getObject("account")?.get("warp_plus") == true
            return account.copy(license = license, warpPlus = warpPlus)
        } catch (e: Exception) {
            Logs.w("WARP: license apply failed", e)
            return account.copy(license = license)
        }
    }

    /** POST /reg with host fallback; any HTTP answer ends the search. */
    private fun postReg(body: String): WarpHttpResponse {
        val failed = mutableListOf<String>()
        for (host in WarpEndpoints.API_HOSTS) {
            try {
                val connection = openConnection("${host}/${WarpApi.VERSION}/reg", method = "POST")
                connection.outputStream.use {
                    it.write(body.toByteArray())
                }
                activeHost = host
                return readResponse(connection)
            } catch (e: Exception) {
                Logs.w("WARP: API host $host unreachable", e)
                failed.add(host)
            }
        }
        throw WarpException("network error: ${failed.joinToString("; ")}")
    }

    private fun openConnection(
        url: String,
        method: String,
        bearer: String? = null,
    ): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", WarpApi.USER_AGENT)
        connection.setRequestProperty("CF-Client-Version", WarpApi.CLIENT_VERSION)
        if (bearer != null) {
            connection.setRequestProperty("Authorization", "Bearer $bearer")
        }
        return connection
    }

    private fun readResponse(connection: HttpURLConnection): WarpHttpResponse {
        val code = connection.responseCode
        val body = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        return WarpHttpResponse(code, body)
    }

    private fun jsonString(value: String): String {
        val escaped = buildString {
            for (c in value) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
                }
            }
        }
        return "\"$escaped\""
    }
}

object WarpApi {
    const val VERSION = "v0a2158"
    const val CLIENT_VERSION = "a-7.21-0721"
    const val USER_AGENT = "okhttp/3.12.1"
}

private class WarpHttpResponse(val statusCode: Int, val body: String)
