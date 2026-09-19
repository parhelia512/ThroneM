@file:Suppress("UNCHECKED_CAST")

package io.throneproj.thronem.fmt.naive

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.buildHeader
import io.throneproj.thronem.fmt.parseBoxOutbound
import io.throneproj.thronem.fmt.parseBoxTLS
import io.throneproj.thronem.fmt.parseBoxUot
import io.throneproj.thronem.fmt.parseHeader
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.queryParameterNotBlank
import io.throneproj.thronem.ktx.unUrlSafe
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

fun parseNaive(link: String): NaiveBean {
    val url = parseURL(link)
    return NaiveBean().also {
        it.proto = url.scheme.substringAfter("+").substringBefore(":")
    }.apply {
        serverAddress = url.host
        serverPort = url.ports.toIntOrNull() ?: 443
        username = url.username
        password = url.password
        sni = url.queryParameter("sni")
        extraHeaders = url.queryParameterNotBlank("extra-headers")
            ?.unUrlSafe()
            ?.replace("\r\n", "\n")
            .orEmpty()
        insecureConcurrency = url.queryParameterNotBlank("insecure-concurrency")?.toIntOrNull() ?: 0
        name = url.fragment
        initializeDefaultValues()
    }
}

fun NaiveBean.toUri(proxyOnly: Boolean = false): String {
    val builder = newURL(if (proxyOnly) proto else "naive+$proto").apply {
        host = serverAddress
        ports = serverPort.toString()
    }
    if (username.isNotBlank()) {
        builder.username = username
    }
    if (password.isNotBlank()) {
        builder.password = password
    }
    if (!proxyOnly) {
        if (sni.isNotBlank()) {
            builder.addQueryParameter("sni", sni)
        }
        if (extraHeaders.isNotBlank()) {
            builder.addQueryParameter("extra-headers", extraHeaders)
        }
        if (name.isNotBlank()) {
            builder.fragment = name
        }
        if (insecureConcurrency > 0) {
            builder.addQueryParameter("insecure-concurrency", "$insecureConcurrency")
        }
    }
    return builder.string
}

fun buildSingBoxOutboundNaiveBean(bean: NaiveBean): SingBoxOptions.Outbound_NaiveOptions {
    return SingBoxOptions.Outbound_NaiveOptions().apply {
        type = SingBoxOptions.TYPE_NAIVE
        server = bean.serverAddress
        server_port = bean.serverPort
        username = bean.username
        password = bean.password
        if (bean.proto == "quic") {
            quic = true
            quic_congestion_control = bean.quicCongestionControl.blankAsNull()
        }
        extra_headers = bean.extraHeaders.blankAsNull()?.let(::buildHeader)?.toMutableMap()
        bean.insecureConcurrency.takeIf { it > 0 }?.let {
            insecure_concurrency = it
        }
        tls = SingBoxOptions.OutboundTLSOptions().apply {
            enabled = true
            server_name = bean.sni

            if (bean.enableEch) SingBoxOptions.OutboundECHOptions().apply {
                enabled = true
                config = bean.echConfig.blankAsNull()?.lines()?.toMutableList()
                query_server_name = bean.echQueryServerName.blankAsNull()
            }
        }
    }
}

fun parseNaiveOutbound(json: JSONMap): NaiveBean = NaiveBean().apply {
    parseBoxOutbound(json) { key, value ->
        when (key) {
            "username" -> username = value.toString()
            "password" -> password = value.toString()
            "insecure_concurrency" -> value.toString().toIntOrNull()?.let {
                insecureConcurrency = it
            }

            "udp_over_tcp" -> udpOverTcp = parseBoxUot(value)
            "quic" -> if (value.toString().toBoolean()) proto = "quic"
            "quic_congestion_control" -> quicCongestionControl = value.toString()

            "extra_headers" -> (value as? JSONMap)?.let(::parseHeader)?.let {
                extraHeaders = it.mapNotNull { entry ->
                    entry.value.firstOrNull()?.let { value ->
                        entry.key + ":" + value
                    }
                }.joinToString("\n")
            }

            "tls" -> (value as? JSONMap)?.let(::parseBoxTLS)?.let { tlsField ->
                sni = tlsField.server_name.orEmpty()
                tlsField.ech?.let { echField ->
                    enableEch = echField.enabled == true
                    echConfig = echField.config?.joinToString("\n").orEmpty()
                    echQueryServerName = echField.query_server_name.orEmpty()
                }
            }
        }
    }
}
