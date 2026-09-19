@file:Suppress("UNCHECKED_CAST")

package io.throneproj.thronem.fmt.masque

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.wireguard.ensureCidr
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.getStr
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.ktx.applyDefaultValues

const val MASQUE_PROFILE_CLOUDFLARE = "cloudflare"

/**
 * Parses a `masque://` share link: userinfo holds the base64(SEC1 DER) ECDSA
 * private key, host:port the data-plane endpoint. Query carries publickey
 * (base64 PKIX DER of the pinned server key), address (local tunnel
 * addresses), profile, vhttp, mtu, sni, idle_timeout and keep_alive.
 */
fun parseMasque(rawUrl: String): MasqueBean {
    val url = parseURL(rawUrl)
    return MasqueBean().applyDefaultValues().apply {
        privateKey = url.username
        serverAddress = url.host
        serverPort = url.ports.toIntOrNull() ?: defaultPort
        name = url.fragment
        publicKey = url.queryParameter("publickey")
        url.queryParameter("address").takeIf { it.isNotBlank() }?.let { address ->
            for (part in address.split(",")) {
                val cidr = part.trim().ensureCidr()
                if (cidr.isEmpty()) continue
                if (cidr.contains(':')) {
                    localIpv6 = cidr
                } else {
                    localIp = cidr
                }
            }
        }
        url.queryParameter("vhttp").takeIf { it.isNotBlank() }?.let { vhttp = it }
        url.queryParameter("mtu").takeIf { it.isNotBlank() }?.toIntOrNull()?.let { mtu = it }
        url.queryParameter("sni").takeIf { it.isNotBlank() }?.let { sni = it }
        url.queryParameter("idle_timeout").takeIf { it.isNotBlank() }?.let { idleTimeout = it }
        url.queryParameter("keep_alive").takeIf { it.isNotBlank() }?.let { keepAlive = it }
    }
}

fun MasqueBean.toUri(): String {
    val builder = newURL("masque").apply {
        username = privateKey
        host = serverAddress
        ports = serverPort.toString()
        rawPath = "/"
        addQueryParameter("publickey", publicKey)
        val addresses = mutableListOf<String>()
        if (localIp.isNotBlank()) addresses.add(localIp)
        if (localIpv6.isNotBlank()) addresses.add(localIpv6)
        addQueryParameter("address", addresses.joinToString(","))
        addQueryParameter("profile", MASQUE_PROFILE_CLOUDFLARE)
        addQueryParameter("vhttp", vhttp.blankAsNull() ?: "auto")
        if (mtu > 0) addQueryParameter("mtu", mtu.toString())
        sni.blankAsNull()?.let { addQueryParameter("sni", it) }
        idleTimeout.blankAsNull()?.let { addQueryParameter("idle_timeout", it) }
        keepAlive.blankAsNull()?.let { addQueryParameter("keep_alive", it) }
        if (name.isNotBlank()) fragment = name
    }
    return builder.string
}

fun buildSingBoxOutboundMasqueBean(bean: MasqueBean): SingBoxOptions.Outbound_MasqueOptions {
    return SingBoxOptions.Outbound_MasqueOptions().apply {
        type = SingBoxOptions.TYPE_MASQUE
        server = bean.serverAddress
        server_port = bean.serverPort
        profile = MASQUE_PROFILE_CLOUDFLARE
        vhttp = bean.vhttp.blankAsNull() ?: "auto"
        private_key = bean.privateKey
        public_key = bean.publicKey.blankAsNull()
        ip = bean.localIp.blankAsNull()
        ipv6 = bean.localIpv6.blankAsNull()
        mtu = bean.mtu
        idle_timeout = bean.idleTimeout.blankAsNull()
        keep_alive_period = bean.keepAlive.blankAsNull()
        bean.sni.blankAsNull()?.let {
            tls = SingBoxOptions.OutboundTLSOptions().apply {
                enabled = true
                server_name = it
            }
        }
    }
}

fun parseMasqueOutbound(json: JSONMap): MasqueBean? {
    val bean = MasqueBean()
    bean.name = json.getStr("tag").orEmpty()
    bean.serverAddress = json["server"]?.toString() ?: return null
    bean.serverPort = json["server_port"]?.toString()?.toIntOrNull() ?: bean.defaultPort
    bean.privateKey = json["private_key"]?.toString().orEmpty()
    bean.publicKey = json["public_key"]?.toString().orEmpty()
    bean.localIp = json["ip"]?.toString()?.ensureCidr().orEmpty()
    bean.localIpv6 = json["ipv6"]?.toString()?.ensureCidr().orEmpty()
    bean.vhttp = json["vhttp"]?.toString() ?: "auto"
    bean.mtu = json["mtu"]?.toString()?.toIntOrNull() ?: 1280
    bean.idleTimeout = json["idle_timeout"]?.toString().orEmpty()
    bean.keepAlive = json["keep_alive_period"]?.toString().orEmpty()
    (json["tls"] as? JSONMap)?.let { tls ->
        tls["server_name"]?.toString()?.takeIf { it.isNotBlank() }?.let { bean.sni = it }
    }
    // Legacy flat shape (removed in v1.14.0-lx.30) — kept for import compat.
    (json["sni"] as? String)?.takeIf { it.isNotBlank() }?.let { bean.sni = it }
    return bean
}
