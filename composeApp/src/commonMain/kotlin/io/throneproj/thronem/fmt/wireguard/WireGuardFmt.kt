@file:Suppress("UNCHECKED_CAST")

package io.throneproj.thronem.fmt.wireguard

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.listable
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.URL
import io.throneproj.thronem.ktx.b64Decode
import io.throneproj.thronem.ktx.b64EncodeOneLine
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.listByLineOrComma
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import io.throneproj.thronem.ktx.applyDefaultValues
import org.ini4j.Ini
import java.io.StringReader

/**
 * The core's wireguard endpoint `address` is Listable[netip.Prefix] — every
 * entry needs a mask. Bare IPv4 gets /32, bare IPv6 /128; empty and
 * already-suffixed values pass through. Applied to `address` (and
 * `allowed_ips` where stored) of WG/AWG/MASQUE from every import path,
 * mirroring LxBox's `ensureCidr`.
 */
fun String.ensureCidr(): String {
    val s = trim()
    if (s.isEmpty() || s.contains('/')) return s
    return if (s.contains(':')) "$s/128" else "$s/32"
}

fun parseWireGuardConfig(conf: String): List<WireGuardBean> {
    val ini = Ini(StringReader(conf))
    val iface = ini["Interface"] ?: error("Missing 'Interface' selection")
    val bean = WireGuardBean().applyDefaultValues()
    val localAddresses = iface.getAll("Address")
    if (localAddresses.isNullOrEmpty()) error("Empty address in 'Interface' selection")
    bean.localAddress = localAddresses.flatMap {
        it.split(",").map { address ->
            address.trim().ensureCidr()
        }
    }.joinToString("\n")
    bean.privateKey = iface["PrivateKey"].orEmpty()
    bean.mtu = iface["MTU"]?.toIntOrNull() ?: 1408
    bean.listenPort = iface["ListenPort"]?.toIntOrNull() ?: 0
    for ((keyName, keyValue) in iface) {
        val value = keyValue ?: continue
        applyAwgField(bean, keyName.lowercase(), value.toString())
    }
    val peers = ini.getAll("Peer")
    if (peers.isNullOrEmpty()) error("Missing 'Peer' selections")
    val beans = mutableListOf<WireGuardBean>()
    loopPeer@ for (peer in peers) {
        val peerBean = bean.clone()
        for ((keyName, keyValue) in peer) {
            when (keyName.lowercase()) {
                "endpoint" -> {
                    peerBean.serverPort = keyValue.substringAfterLast(":", "").toIntOrNull()
                        ?: continue@loopPeer
                    peerBean.serverAddress = keyValue.substringBeforeLast(":")
                }

                "publickey" -> peerBean.publicKey = keyValue ?: continue@loopPeer
                "presharedkey" -> peerBean.preSharedKey = keyValue
                "persistentkeepalive" -> {
                    peerBean.persistentKeepaliveInterval = keyValue.toIntOrNull() ?: 0
                }

                "reserved" -> peerBean.reserved = keyValue
            }
        }
        beans.add(peerBean.applyDefaultValues())
    }
    if (beans.isEmpty()) error("Empty available peer list")
    return beans
}

/**
 * The core's `reserved` is a JSON number array (WireGuardPeer.Reserved
 * []uint8). Accepts either the "b0,b1,b2" decimal form or a base64 client_id
 * (Cloudflare WARP); null when the value does not carry 3 usable bytes.
 */
fun reservedToIntList(anyStr: String): MutableList<Int>? {
    return try {
        val list = anyStr.listByLineOrComma()
        if (list.size == 3) {
            val out = mutableListOf<Int>()
            for (s in list) {
                val i = s.replace("[", "").replace("]", "").replace(" ", "").toIntOrNull()
                    ?: return null
                out.add(i and 0xFF)
            }
            out
        } else {
            val bytes = anyStr.trim().b64Decode()
            if (bytes.size == 3) bytes.map { it.toInt() and 0xFF }.toMutableList() else null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Applies one AmneziaWG parameter (INI key or URI query name, already
 * lowercased) onto [bean]. Returns true when the key was consumed. Both the
 * raw JSON key ("jmin") and the AWG3 underscore-free form
 * ("contentpaddingaddition") are accepted.
 */
fun applyAwgField(bean: WireGuardBean, key: String, value: String): Boolean {
    val v = value.trim()
    if (v.isEmpty()) return false
    when (key) {
        "jc" -> v.toIntOrNull()?.let { bean.awgJc = it }
        "jmin" -> v.toIntOrNull()?.let { bean.awgJmin = it }
        "jmax" -> v.toIntOrNull()?.let { bean.awgJmax = it }
        "s1" -> v.toIntOrNull()?.let { bean.awgS1 = it }
        "s2" -> v.toIntOrNull()?.let { bean.awgS2 = it }
        "h1" -> bean.awgH1 = v
        "h2" -> bean.awgH2 = v
        "h3" -> bean.awgH3 = v
        "h4" -> bean.awgH4 = v
        "i1" -> bean.awgI1 = v
        "i2" -> bean.awgI2 = v
        "i3" -> bean.awgI3 = v
        "i4" -> bean.awgI4 = v
        "i5" -> bean.awgI5 = v
        "id" -> bean.awgId = v
        "ip" -> bean.awgIp = v
        "ib" -> bean.awgIb = v
        else -> return false
    }
    return true
}

/** Builds a `wireguard://` share link (SFA/WireSock compatible, AWG params included). */
fun WireGuardBean.toWireguardUri(): String {
    val builder = newURL("wireguard").apply {
        username = privateKey
        host = serverAddress
        ports = serverPort.toString()
        rawPath = "/"
        addQueryParameter("publickey", publicKey)
        addQueryParameter("address", localAddress.listByLineOrComma().joinToString(","))
        addQueryParameter("allowedips", "0.0.0.0/0,::/0")
        if (mtu > 0) addQueryParameter("mtu", mtu.toString())
        reserved.blankAsNull()?.let { addQueryParameter("reserved", it) }
        if (persistentKeepaliveInterval > 0) {
            addQueryParameter("keepalive", persistentKeepaliveInterval.toString())
        }
        if (awgJc > 0) addQueryParameter("jc", awgJc.toString())
        if (awgJmin > 0) addQueryParameter("jmin", awgJmin.toString())
        if (awgJmax > 0) addQueryParameter("jmax", awgJmax.toString())
        if (awgS1 > 0) addQueryParameter("s1", awgS1.toString())
        if (awgS2 > 0) addQueryParameter("s2", awgS2.toString())
        awgH1.blankAsNull()?.let { addQueryParameter("h1", it) }
        awgH2.blankAsNull()?.let { addQueryParameter("h2", it) }
        awgH3.blankAsNull()?.let { addQueryParameter("h3", it) }
        awgH4.blankAsNull()?.let { addQueryParameter("h4", it) }
        awgI1.blankAsNull()?.let { addQueryParameter("i1", it) }
        awgI2.blankAsNull()?.let { addQueryParameter("i2", it) }
        awgI3.blankAsNull()?.let { addQueryParameter("i3", it) }
        awgI4.blankAsNull()?.let { addQueryParameter("i4", it) }
        awgI5.blankAsNull()?.let { addQueryParameter("i5", it) }
        awgId.blankAsNull()?.let { addQueryParameter("id", it) }
        awgIp.blankAsNull()?.let { addQueryParameter("ip", it) }
        awgIb.blankAsNull()?.let { addQueryParameter("ib", it) }
        if (name.isNotBlank()) fragment = name
    }
    return builder.string
}

/**
 * Parses a `wireguard://` share link: userinfo holds the base64 private key,
 * host:port the peer endpoint. Query carries publickey/address/mtu/reserved/
 * keepalive plus AmneziaWG parameters.
 */
fun parseWireGuardUri(rawUrl: String): WireGuardBean {
    val url: URL = parseURL(rawUrl)
    return WireGuardBean().applyDefaultValues().apply {
        privateKey = url.username
        serverAddress = url.host
        serverPort = url.ports.toIntOrNull() ?: defaultPort
        name = url.fragment
        publicKey = url.queryParameter("publickey")
        url.queryParameter("address").takeIf { it.isNotBlank() }?.let {
            localAddress = it.split(",").map { a -> a.trim().ensureCidr() }.joinToString("\n")
        }
        url.queryParameter("mtu").takeIf { it.isNotBlank() }?.toIntOrNull()?.let { mtu = it }
        url.queryParameter("reserved").takeIf { it.isNotBlank() }?.let { reserved = it }
        url.queryParameter("keepalive").takeIf { it.isNotBlank() }?.toIntOrNull()?.let {
            persistentKeepaliveInterval = it
        }
        url.queryParameter("presharedkey").takeIf { it.isNotBlank() }?.let { preSharedKey = it }
        for (key in AWG_URI_KEYS) {
            applyAwgField(this, key, url.queryParameter(key))
        }
    }
}

private val AWG_URI_KEYS = listOf(
    "jc", "jmin", "jmax", "s1", "s2", "h1", "h2", "h3", "h4",
    "i1", "i2", "i3", "i4", "i5", "id", "ip", "ib",
)

/** Emits an AWG option only when it was actually configured. */
private fun SingBoxOptions.Endpoint_WireGuardOptions.applyAwgFields(bean: WireGuardBean) {
    if (!bean.hasAwg) return
    if (bean.awgJc > 0) jc = bean.awgJc
    if (bean.awgJmin > 0) jmin = bean.awgJmin
    if (bean.awgJmax > 0) jmax = bean.awgJmax
    if (bean.awgS1 > 0) s1 = bean.awgS1
    if (bean.awgS2 > 0) s2 = bean.awgS2
    bean.awgH1.blankAsNull()?.let { h1 = it }
    bean.awgH2.blankAsNull()?.let { h2 = it }
    bean.awgH3.blankAsNull()?.let { h3 = it }
    bean.awgH4.blankAsNull()?.let { h4 = it }
    bean.awgI1.blankAsNull()?.let { i1 = it }
    bean.awgI2.blankAsNull()?.let { i2 = it }
    bean.awgI3.blankAsNull()?.let { i3 = it }
    bean.awgI4.blankAsNull()?.let { i4 = it }
    bean.awgI5.blankAsNull()?.let { i5 = it }
    bean.awgId.blankAsNull()?.let { id = it }
    bean.awgIp.blankAsNull()?.let { ip = it }
    bean.awgIb.blankAsNull()?.let { ib = it }
}

fun buildSingBoxEndpointWireGuardBean(bean: WireGuardBean): SingBoxOptions.Endpoint_WireGuardOptions {
    return SingBoxOptions.Endpoint_WireGuardOptions().apply {
        type = SingBoxOptions.TYPE_WIREGUARD
        peers = mutableListOf(
            SingBoxOptions.WireGuardPeer().apply {
                address = bean.serverAddress
                port = bean.serverPort
                public_key = bean.publicKey
                pre_shared_key = bean.preSharedKey.blankAsNull()
                allowed_ips = mutableListOf(
                    "0.0.0.0/0",
                    "::/0",
                )
                bean.persistentKeepaliveInterval.takeIf { it > 0 }?.let {
                    persistent_keepalive_interval = it
                }
                bean.reserved.blankAsNull()?.let { reserved = reservedToIntList(it) }
            },
        )
        listen_port = bean.listenPort.takeIf { it > 0 }
        address = bean.localAddress.listByLineOrComma().toMutableList()
        private_key = bean.privateKey
        mtu = bean.mtu
        applyAwgFields(bean)
    }
}

fun parseWireGuardEndpoint(json: JSONMap): WireGuardBean? {
    val peer = (json["peers"] as? List<*>)?.firstOrNull() as? JSONMap ?: return null

    val bean = WireGuardBean()
    bean.name = json["tag"].toString()
    bean.mtu = json["mtu"]?.toString()?.toIntOrNull() ?: 0
    bean.localAddress = listable<String>(json["address"])?.map { it.ensureCidr() }?.joinToString("\n").orEmpty()
    bean.listenPort = json["listen_port"]?.toString()?.toIntOrNull() ?: 0
    bean.privateKey = json["private_key"]?.toString().orEmpty()
    for (awgKey in AWG_OPTION_KEYS) {
        val value = json[awgKey] ?: continue
        applyAwgField(bean, awgKey, value.toString())
    }

    for (entry in peer) {
        val value = entry.value ?: continue
        when (entry.key) {
            "address" -> bean.serverAddress = value.toString()
            "port" -> bean.serverPort = value.toString().toInt()
            "public_key" -> bean.publicKey = value.toString()
            "pre_shared_key" -> bean.preSharedKey = value.toString()
            "persistent_keepalive_interval" -> value.toString().toIntOrNull()?.let {
                bean.persistentKeepaliveInterval = it
            }

            "reserved" -> bean.reserved = when (value) {
                is String -> value

                is List<*> -> value.joinToString(",") {
                    it.toString().trim()
                }

                else -> ""
            }
        }
    }

    return bean
}

private val AWG_OPTION_KEYS = listOf(
    "jc", "jmin", "jmax", "s1", "s2", "h1", "h2", "h3", "h4",
    "i1", "i2", "i3", "i4", "i5", "id", "ip", "ib",
)
