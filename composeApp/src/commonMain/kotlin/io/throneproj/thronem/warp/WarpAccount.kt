package io.throneproj.thronem.warp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cached Cloudflare WARP account. The X25519 private key is generated on
 * device during registration ([WarpClient.register]) and never leaves it.
 * Serialized to DataStore as JSON so re-registration is only needed when the
 * user explicitly forces a new account.
 */
@Serializable
data class WarpAccount(
    val privKey: String,
    val peerPub: String,
    val clientV4: String,
    val clientV6: String,
    /** base64 `config.client_id` (3 bytes) → WireGuard reserved. */
    val clientId: String,
    val accountId: String,
    val deviceId: String,
    /** Bearer token of the device; needed for the license PATCH. */
    val token: String,
    val endpoint: String = WarpEndpoints.DEFAULT_ENDPOINT,
    val createdAt: String = "",
    val license: String? = null,
    val warpPlus: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String): WarpAccount? {
            return try {
                json.decodeFromString(serializer(), raw)
            } catch (_: Exception) {
                null
            }
        }
    }
}
