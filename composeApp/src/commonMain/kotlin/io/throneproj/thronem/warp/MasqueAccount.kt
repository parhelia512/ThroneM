package io.throneproj.thronem.warp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cached Cloudflare WARP account for the MASQUE transport, ported from LxBox's
 * MasqueAccount. A separate model from [WarpAccount] (X25519/WireGuard):
 * MASQUE uses different crypto (ECDSA P-256 DER) and has no reserved/AWG.
 * The private key is generated on device ([MasqueKeys]) and never leaves it —
 * Cloudflare only ever sees the public part (PATCH enroll).
 *
 * The HTTP version (h3/h2) is deliberately NOT stored here: it is a property
 * of the generated node, not of the registration. The same credentials build
 * both h3 and h2 nodes.
 */
@Serializable
data class MasqueAccount(
    /** base64(SEC1 DER) of our ECDSA private key. SECRET: never log. */
    val privKeyDer: String,
    /** base64(PKIX DER) of the server ECDSA pubkey (`peers[0].public_key`, pinning). */
    val serverPubDer: String,
    /** Interface address v4 (CIDR), e.g. `172.16.0.2/32`. */
    val clientV4: String,
    /** Interface address v6 (CIDR). */
    val clientV6: String,
    /** Data-plane endpoint IP (without the `:0` stub), e.g. `162.159.198.1`. */
    val server: String = DEFAULT_SERVER,
    /** Data-plane port, usually 443. */
    val port: Int = DEFAULT_PORT,
    val deviceId: String,
    /** Bearer token of the device. SECRET: never log. */
    val token: String,
    val createdAt: String = "",
    /** TLS SNI override; blank = core default. */
    val sni: String = "",
    /** idle-suspend (Go duration, e.g. `5m`); blank = core default. */
    val idleTimeout: String = "",
    /** QUIC keep-alive period (Go duration, e.g. `30s`); blank = core default. */
    val keepAlive: String = "",
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    fun copyWith(sni: String? = null, idleTimeout: String? = null, keepAlive: String? = null) =
        copy(
            sni = sni ?: this.sni,
            idleTimeout = idleTimeout ?: this.idleTimeout,
            keepAlive = keepAlive ?: this.keepAlive,
        )

    companion object {
        /** Default data-plane endpoint for MASQUE (QUIC). */
        const val DEFAULT_SERVER = "162.159.198.1"
        const val DEFAULT_PORT = 443

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String): MasqueAccount? {
            return try {
                json.decodeFromString(serializer(), raw)
            } catch (_: Exception) {
                null
            }
        }
    }
}
