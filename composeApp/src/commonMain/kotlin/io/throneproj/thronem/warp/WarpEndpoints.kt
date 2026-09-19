package io.throneproj.thronem.warp

import java.security.SecureRandom

/**
 * Cloudflare WARP endpoint data ported from the LxBox warp_endpoints.json
 * asset. Used for the API host fallback order and for picking a random
 * unblocked `ip:port` when the AmneziaWG obfuscation is enabled (plain WARP
 * keeps `engage.cloudflareclient.com:2408`).
 */
object WarpEndpoints {

    const val DEFAULT_ENDPOINT = "engage.cloudflareclient.com:2408"
    const val DEFAULT_PEER_PORT = 2408
    const val DEFAULT_KEEPALIVE = 25
    const val WARP_MTU = 1280

    val API_HOSTS = listOf(
        "https://api.devices.cloudflare.com",
        "https://api.cloudflareclient.com",
    )

    /** WireGuard endpoint presets, first one recommended. */
    val ENDPOINT_PRESETS = listOf(
        "engage.cloudflareclient.com:2408",
        "engage.cloudflareclient.com:500",
        "engage.cloudflareclient.com:1701",
        "engage.cloudflareclient.com:4500",
        "162.159.192.192:934",
        "162.159.192.1:2408",
        "162.159.193.1:2408",
        "188.114.96.1:2408",
    )

    private val V4_CIDR_BLOCKS = listOf(
        "162.159.192.0/24",
        "162.159.193.0/24",
        "162.159.195.0/24",
        "188.114.96.0/22",
    )

    private val V6_CIDR_BLOCKS = listOf(
        "2606:4700:d0::/64",
        "2606:4700:d1::/64",
    )

    private val PORTS = listOf(2408, 500, 1701, 4500)

    private val PORTS_EXTRA = listOf(
        854, 859, 864, 878, 880, 890, 891, 894, 903, 908, 928, 934, 939, 942,
        943, 945, 946, 955, 968, 987, 988, 1002, 1010, 1014, 1018, 1070, 1074,
        1180, 1387, 1843, 2371, 2506, 3138, 3476, 3581, 3854, 4177, 4198, 4233,
        5279, 5956, 7103, 7152, 7156, 7281, 7559, 8319, 8742, 8854, 8886,
    )

    /** Masquerade domains for AWG junk packets (no Cloudflare domains on purpose). */
    val SNI_POOL = listOf(
        "www.google.com", "www.microsoft.com", "www.bing.com", "www.apple.com",
        "www.wikipedia.org", "cdn.jsdelivr.net", "aws.amazon.com", "deepseek.com",
    )

    // ─── MASQUE (§130/§305/§420, from the LxBox warp_endpoints.json asset) ───

    /** Hosts common to both HTTP transports, first one recommended. */
    val MASQUE_HOSTS_PRESET = listOf("162.159.198.2", "162.159.199.2")
    const val RECOMMENDED_MASQUE_HOST = "162.159.198.2"

    /** Live h3-only hosts (the .1 addresses answer HTTP/3 only). */
    private val MASQUE_H3_HOSTS_EXTRA = listOf("162.159.198.1", "162.159.199.1")

    /** Device-verified MASQUE ports; the h3 and h2 sets are identical. */
    val MASQUE_PORTS = listOf(443, 500, 1701, 4500, 4443, 8443, 8095)

    /** v4 block for manual/random h2 endpoints, minus the h3-only hosts. */
    private val MASQUE_V4_CIDR = listOf("162.159.198.0/24", "162.159.199.0/24")
    private val MASQUE_H2_EXCLUDE = MASQUE_H3_HOSTS_EXTRA

    /**
     * MASQUE SNI pool — Cloudflare domains included on purpose: the SNI lives
     * inside the TLS to Cloudflare itself, so its own domains are the natural
     * choice (unlike the AWG junk-packet pool above).
     */
    val MASQUE_SNI_POOL = listOf(
        "consumer-masque.cloudflareclient.com",
        "www.cloudflare.com",
        "www.google.com",
        "www.microsoft.com",
        "www.bing.com",
        "www.apple.com",
        "www.wikipedia.org",
        "cdn.jsdelivr.net",
        "aws.amazon.com",
        "deepseek.com",
    )
    const val RECOMMENDED_MASQUE_SNI = "consumer-masque.cloudflareclient.com"

    /** Combobox hosts for the transport: `h3` adds the h3-only addresses. */
    fun masqueHostsFor(network: String): List<String> =
        if (network == "h3") MASQUE_HOSTS_PRESET + MASQUE_H3_HOSTS_EXTRA
        else MASQUE_HOSTS_PRESET

    /** Random MASQUE IP: live h3 hosts for `h3`, block-minus-exclusions otherwise. */
    fun randomMasqueIp(network: String): String? {
        if (network == "h3") {
            val hosts = MASQUE_HOSTS_PRESET + MASQUE_H3_HOSTS_EXTRA
            return hosts[random.nextInt(hosts.size)]
        }
        val cidr = MASQUE_V4_CIDR[random.nextInt(MASQUE_V4_CIDR.size)]
        while (true) {
            val ip = randomIpInV4Cidr(cidr) ?: return null
            if (ip !in MASQUE_H2_EXCLUDE) return ip
        }
    }

    fun randomMasquePort(): Int = MASQUE_PORTS[random.nextInt(MASQUE_PORTS.size)]

    private fun randomIpInV4Cidr(cidr: String): String? {
        val slash = cidr.lastIndexOf('/')
        val prefix = cidr.substring(slash + 1).toIntOrNull() ?: return null
        if (prefix < 8 || prefix > 32) return null
        val base = cidr.substring(0, slash).split('.').map { it.toInt() }
        val range = 1 shl (32 - prefix)
        val offset = random.nextInt(range - 2) + 1
        val third = base[2] + (offset shr 8)
        val fourth = offset and 0xFF
        return "${base[0]}.${base[1]}.$third.$fourth"
    }

    private val random = SecureRandom()

    fun randomSni(): String = SNI_POOL[random.nextInt(SNI_POOL.size)]

    /** Random SNI from the MASQUE pool (may be a Cloudflare domain). */
    fun randomMasqueSni(): String = MASQUE_SNI_POOL[random.nextInt(MASQUE_SNI_POOL.size)]

    /**
     * Random `ip:port` from the Cloudflare WARP blocks. [allowV6] only when
     * the system has IPv6 — a v6 endpoint is useless otherwise.
     */
    fun randomEndpoint(allowV6: Boolean = false): String {
        val port = (PORTS + PORTS_EXTRA)[random.nextInt(PORTS.size + PORTS_EXTRA.size)]
        val host = if (allowV6 && random.nextInt(4) == 0) {
            randomV6Host()
        } else {
            randomV4Host()
        }
        return "$host:$port"
    }

    private fun randomV4Host(): String {
        val cidr = V4_CIDR_BLOCKS[random.nextInt(V4_CIDR_BLOCKS.size)]
        val slash = cidr.lastIndexOf('/')
        val base = cidr.substring(0, slash).split('.').map { it.toInt() }
        // /22 spans 4 x /24; pick an address inside the block (skip network+1 for safety).
        val range = 1 shl (32 - cidr.substring(slash + 1).toInt())
        val offset = random.nextInt(range - 2) + 1
        val third = base[2] + (offset shr 8)
        val fourth = offset and 0xFF
        return "${base[0]}.${base[1]}.$third.$fourth"
    }

    private fun randomV6Host(): String {
        val cidr = V6_CIDR_BLOCKS[random.nextInt(V6_CIDR_BLOCKS.size)]
        val base = cidr.substringBefore('/')
        val groupCount = base.split(':').count { it.isNotEmpty() }
        val builder = StringBuilder(base.trimEnd(':'))
        if (!builder.endsWith("::")) builder.append(':')
        for (i in groupCount until 8) {
            builder.append(':')
            if (i < 7) {
                builder.append(Integer.toHexString(random.nextInt(0x10000)))
            }
        }
        return builder.toString().replace(":::", "::")
    }
}
