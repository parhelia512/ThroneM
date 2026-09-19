package io.throneproj.thronem.ktx

/**
 * Share-link URL model replacing the old libcore `Libcore.parseURL`/`newURL`
 * JNI API (which came from sing-box's common/url package).
 *
 * Semantics kept from that implementation:
 *  - [queryParameter] returns the percent-DEcoded value ('+' becomes space,
 *    matching Go's `url.Values.Get`); [queryParameterUnescape] decodes once
 *    more, for values that were double-encoded.
 *  - [addQueryParameter]/[setQueryParameter] take decoded values and escape
 *    them like Go's `url.Values.Encode` when serializing.
 *  - [ports] keeps the raw port section, including hysteria ranges ("443-8443").
 *  - userinfo is percent-decoded on parse and encoded on serialize; IPv6 hosts
 *    are stored without brackets and bracketed again by [string].
 *  - the fragment is decoded on parse and encoded on serialize.
 */
class URL private constructor() {

    var scheme: String = ""
    var host: String = ""
    var ports: String = ""
    var username: String = ""
    var password: String = ""
    var rawPath: String = ""
    var fragment: String = ""

    private var rawQuery: String = ""

    /** Decoded path; setting it stores the percent-encoded form in [rawPath]. */
    var path: String
        get() = rawPath.percentDecode()
        set(value) {
            rawPath = value.percentEncode(PATH_SAFE)
        }

    val string: String
        get() = buildString {
            append(scheme)
            append("://")
            if (username.isNotEmpty() || password.isNotEmpty()) {
                append(username.percentEncode(USERINFO_SAFE))
                if (password.isNotEmpty()) {
                    append(':')
                    append(password.percentEncode(USERINFO_SAFE))
                }
                append('@')
            }
            if (host.contains(':')) {
                append('[').append(host).append(']')
            } else {
                append(host)
            }
            if (ports.isNotEmpty()) {
                append(':').append(ports)
            }
            if (rawPath.isNotEmpty()) {
                if (!rawPath.startsWith("/")) append('/')
                append(rawPath)
            }
            if (rawQuery.isNotEmpty()) {
                append('?').append(rawQuery)
            }
            if (fragment.isNotEmpty()) {
                append('#').append(fragment.percentEncode(FRAGMENT_SAFE))
            }
        }

    /**
     * Combined "host:port" form (bracketed for IPv6). Assigning splits the
     * host and port sections apart.
     */
    var fullHost: String
        get() = buildString {
            if (host.contains(':')) {
                append('[').append(host).append(']')
            } else {
                append(host)
            }
            if (ports.isNotEmpty()) append(':').append(ports)
        }
        set(value) {
            val trimmed = value.trim()
            if (trimmed.startsWith("[")) {
                val close = trimmed.indexOf(']')
                if (close > 0) {
                    host = trimmed.substring(1, close)
                    ports = if (trimmed.length > close + 1) trimmed.substring(close + 2) else ""
                    return
                }
            }
            val colon = trimmed.indexOf(':')
            val lastColon = trimmed.lastIndexOf(':')
            if (colon >= 0 && colon == lastColon) {
                host = trimmed.substring(0, colon)
                ports = trimmed.substring(colon + 1)
            } else {
                host = trimmed
                ports = ""
            }
        }

    /**
     * First value for [key], percent-decoded with '+' read as space
     * (matching the old libcore `url.Values.Get`); empty string when absent.
     */
    fun queryParameter(key: String): String {
        for (pair in rawQuery.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val k = if (eq >= 0) pair.substring(0, eq) else pair
            if (k.percentDecode(plusAsSpace = true) == key) {
                return if (eq >= 0) pair.substring(eq + 1).percentDecode(plusAsSpace = true) else ""
            }
        }
        return ""
    }

    /** Like [queryParameter] but decodes once more, for double-encoded values. */
    fun queryParameterUnescape(key: String): String =
        queryParameter(key).percentDecode()

    /** Appends a key/value pair; both are escaped like Go's `url.Values.Encode`. */
    fun addQueryParameter(key: String, value: String) {
        if (rawQuery.isNotEmpty()) rawQuery += "&"
        rawQuery += "${key.queryEncode()}=${value.queryEncode()}"
    }

    /** Replaces every value of [key] with a single raw pair. */
    fun setQueryParameter(key: String, value: String) {
        removeQueryParameter(key)
        addQueryParameter(key, value)
    }

    fun removeQueryParameter(key: String) {
        if (rawQuery.isEmpty()) return
        val kept = rawQuery.split('&').filter { pair ->
            if (pair.isEmpty()) {
                false
            } else {
                val k = pair.substringBefore('=').percentDecode(plusAsSpace = true)
                k != key
            }
        }
        rawQuery = kept.joinToString("&")
    }

    override fun toString(): String = string

    companion object {

        /** Parses a share link; throws [IllegalArgumentException] on garbage. */
        fun parse(raw: String): URL {
            val trimmed = raw.trim()
            val schemeEnd = trimmed.indexOf("://")
            if (schemeEnd <= 0) throw IllegalArgumentException("invalid url: $raw")
            val url = URL()
            url.scheme = trimmed.substring(0, schemeEnd)
            var rest = trimmed.substring(schemeEnd + 3)

            val hash = rest.indexOf('#')
            if (hash >= 0) {
                url.fragment = rest.substring(hash + 1).percentDecode()
                rest = rest.substring(0, hash)
            }

            val query = rest.indexOf('?')
            if (query >= 0) {
                url.rawQuery = rest.substring(query + 1)
                rest = rest.substring(0, query)
            }

            val slash = rest.indexOf('/')
            var authority = rest
            if (slash >= 0) {
                url.rawPath = rest.substring(slash)
                authority = rest.substring(0, slash)
            }

            val at = authority.lastIndexOf('@')
            if (at >= 0) {
                val userInfo = authority.substring(0, at)
                authority = authority.substring(at + 1)
                val colon = userInfo.indexOf(':')
                if (colon >= 0) {
                    url.username = userInfo.substring(0, colon).percentDecode()
                    url.password = userInfo.substring(colon + 1).percentDecode()
                } else {
                    url.username = userInfo.percentDecode()
                }
            }

            if (authority.isEmpty()) throw IllegalArgumentException("empty host: $raw")

            if (authority.startsWith("[")) {
                val close = authority.indexOf(']')
                if (close < 0) throw IllegalArgumentException("invalid ipv6 host: $raw")
                url.host = authority.substring(1, close)
                if (authority.length > close + 1) {
                    if (authority[close + 1] != ':') {
                        throw IllegalArgumentException("invalid host: $raw")
                    }
                    url.ports = authority.substring(close + 2)
                }
            } else {
                val colon = authority.indexOf(':')
                val lastColon = authority.lastIndexOf(':')
                if (colon >= 0 && colon == lastColon) {
                    url.host = authority.substring(0, colon)
                    url.ports = authority.substring(colon + 1)
                } else {
                    // Bare IPv6 literal or a port section with ranges joined by ':'.
                    url.host = authority
                }
            }
            return url
        }

        /** Builds an empty URL for the given scheme, ready for field assignment. */
        fun of(scheme: String): URL = URL().apply { this.scheme = scheme }
    }
}

fun parseURL(raw: String): URL = URL.parse(raw)

fun newURL(scheme: String): URL = URL.of(scheme)

private val USERINFO_SAFE = Regex("[A-Za-z0-9-._~!$&'()*+,;=]")
private val PATH_SAFE = Regex("[A-Za-z0-9-._~!$&'()*+,;=:@/]")
private val FRAGMENT_SAFE = Regex("[A-Za-z0-9-._~!$&'()*+,;=:@/?]")

private val HEX = "0123456789ABCDEF".toCharArray()

private fun String.percentEncode(safe: Regex): String {
    val bytes = toByteArray(Charsets.UTF_8)
    val out = StringBuilder(bytes.size)
    for (b in bytes) {
        val c = (b.toInt() and 0xFF).toChar()
        if (b in 0 until 0x80 && safe.matches(c.toString())) {
            out.append(c)
        } else {
            out.append('%')
            out.append(HEX[(b.toInt() shr 4) and 0xF])
            out.append(HEX[b.toInt() and 0xF])
        }
    }
    return out.toString()
}

private fun String.percentDecode(plusAsSpace: Boolean = false): String {
    if (plusAsSpace) {
        if (indexOf('%') < 0 && indexOf('+') < 0) return this
    } else if (indexOf('%') < 0) {
        return this
    }
    val bytes = ByteArray(length)
    var n = 0
    var i = 0
    while (i < length) {
        val c = this[i]
        if (plusAsSpace && c == '+') {
            bytes[n++] = ' '.code.toByte()
            i++
        } else if (c == '%' && i + 2 <= lastIndex) {
            val hi = Character.digit(this[i + 1], 16)
            val lo = Character.digit(this[i + 2], 16)
            if (hi >= 0 && lo >= 0) {
                bytes[n++] = ((hi shl 4) or lo).toByte()
                i += 3
                continue
            }
            bytes[n++] = c.code.toByte()
            i++
        } else {
            bytes[n++] = c.code.toByte()
            i++
        }
    }
    return String(bytes, 0, n, Charsets.UTF_8)
}

/**
 * Escapes a query component like Go's `url.Values.Encode`: only unreserved
 * characters survive, space becomes '+'.
 */
private fun String.queryEncode(): String {
    val bytes = toByteArray(Charsets.UTF_8)
    val out = StringBuilder(bytes.size)
    for (b in bytes) {
        val c = (b.toInt() and 0xFF).toChar()
        when {
            b.toInt() == ' '.code -> out.append('+')
            b in 0 until 0x80 && (c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~') -> out.append(c)
            else -> {
                out.append('%')
                out.append(HEX[(b.toInt() shr 4) and 0xF])
                out.append(HEX[b.toInt() and 0xF])
            }
        }
    }
    return out.toString()
}
