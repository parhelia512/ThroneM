package io.throneproj.thronem.fmt.socks

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.parseBoxOutbound
import io.throneproj.thronem.fmt.parseBoxUot
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.b64DecodeToString
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

fun parseSOCKS(link: String): SOCKSBean {
    val url = parseURL(link)
    return SOCKSBean().apply {
        protocol = when (url.scheme) {
            "socks4" -> SOCKSBean.PROTOCOL_SOCKS4
            "socks4a" -> SOCKSBean.PROTOCOL_SOCKS4A
            else -> SOCKSBean.PROTOCOL_SOCKS5
        }
        name = url.fragment
        serverAddress = url.host
        serverPort = url.ports.toIntOrNull() ?: 1080
        username = url.username
        password = url.password
        // v2rayN fmt
        if (password.isBlank() && username.isNotBlank()) {
            try {
                val n = username.b64DecodeToString()
                username = n.substringBefore(":")
                password = n.substringAfter(":")
            } catch (_: Exception) {
            }
        }
    }
}

fun SOCKSBean.toUri(): String {
    val builder = newURL("socks${protocolVersion()}").apply {
        host = serverAddress
        ports = serverPort.toString()
    }
    if (username.isNotBlank()) builder.username = username
    if (password.isNotBlank()) builder.password = password
    if (name.isNotBlank()) builder.fragment = name
    return builder.string
}

fun buildSingBoxOutboundSocksBean(bean: SOCKSBean): SingBoxOptions.Outbound_SOCKSOptions {
    return SingBoxOptions.Outbound_SOCKSOptions().apply {
        type = SingBoxOptions.TYPE_SOCKS
        server = bean.serverAddress
        server_port = bean.serverPort
        username = bean.username
        password = bean.password
        version = bean.protocolVersionName()
    }
}

fun parseSocksOutbound(json: JSONMap): SOCKSBean = SOCKSBean().apply {
    parseBoxOutbound(json) { key, value ->
        when (key) {
            "username" -> username = value.toString()
            "password" -> password = value.toString()
            "version" -> protocol = when (value.toString()) {
                "4" -> SOCKSBean.PROTOCOL_SOCKS4
                "4a" -> SOCKSBean.PROTOCOL_SOCKS4A
                else -> SOCKSBean.PROTOCOL_SOCKS5
            }

            "udp_over_tcp" -> udpOverTcp = parseBoxUot(value)
        }
    }
}