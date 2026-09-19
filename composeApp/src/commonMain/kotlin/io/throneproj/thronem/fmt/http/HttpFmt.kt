package io.throneproj.thronem.fmt.http

import io.throneproj.thronem.fmt.parseBoxOutbound
import io.throneproj.thronem.fmt.parseBoxTLS
import io.throneproj.thronem.fmt.parseHeader
import io.throneproj.thronem.fmt.v2ray.setTLS
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.toJSONMap
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

fun parseHttp(link: String): HttpBean = HttpBean().apply {
    val url = parseURL(link)

    serverAddress = url.host
    serverPort = url.ports.toIntOrNull() ?: if (url.scheme == "https") 443 else 80
    username = url.username
    password = url.password
    sni = url.queryParameter("sni")
    name = url.fragment
    setTLS(url.scheme == "https")
    path = url.path
}

fun HttpBean.toUri(): String {
    val url = newURL(if (isTLS) "https" else "http").apply {
        host = serverAddress
    }

    if (serverPort in 1..65535) {
        url.ports = serverPort.toString()
    }

    username.blankAsNull()?.let { url.username = it }
    password.blankAsNull()?.let { url.password = it }
    path.blankAsNull()?.let { url.rawPath = it }
    sni.blankAsNull()?.let { url.addQueryParameter("sni", it) }
    name.blankAsNull()?.let { url.fragment = it }

    return url.string
}

fun parseHttpOutbound(json: JSONMap): HttpBean = HttpBean().apply {
    parseBoxOutbound(json) { key, value ->
        when (key) {
            "username" -> username = value.toString()
            "password" -> password = value.toString()
            "path" -> path = value.toString()
            "headers" -> (value as? Map<*, *>)?.let {
                headers = parseHeader(it).map { entry ->
                    entry.key + ":" + entry.value.joinToString(",")
                }.joinToString("\n")
            }

            "tls" -> {
                val tlsJson = (value as? Map<*, *>)?.let {
                    toJSONMap(it)
                } ?: return@parseBoxOutbound
                val tls = parseBoxTLS(tlsJson)
                if (tls.enabled != true) return@parseBoxOutbound

                setTLS(true)
                sni = tls.server_name.orEmpty()
                alpn = tls.alpn?.joinToString(",").orEmpty()
                utlsFingerprint = tls.utls?.fingerprint.orEmpty()
                allowInsecure = tls.insecure == true
                disableSNI = tls.disable_sni == true
                certificates = tls.certificate?.joinToString("\n").orEmpty()
                certPublicKeySha256 =
                    tls.certificate_public_key_sha256?.joinToString("\n").orEmpty()
                clientCert = tls.client_certificate?.joinToString("\n").orEmpty()
                clientKey = tls.client_key?.joinToString("\n").orEmpty()
                tls.reality?.let {
                    realityPublicKey = it.public_key.orEmpty()
                    realityShortID = it.short_id.orEmpty()
                }
                tls.ech?.let {
                    ech = it.enabled == true
                    echConfig = it.config?.joinToString("\n").orEmpty()
                }
            }
        }
    }
}