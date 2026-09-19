@file:Suppress("UNCHECKED_CAST")

package io.throneproj.thronem.fmt.v2ray

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound
import io.throneproj.thronem.fmt.SingBoxOptions.OutboundECHOptions
import io.throneproj.thronem.fmt.SingBoxOptions.OutboundRealityOptions
import io.throneproj.thronem.fmt.SingBoxOptions.OutboundTLSOptions
import io.throneproj.thronem.fmt.SingBoxOptions.OutboundUTLSOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound_HTTPOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound_TrojanOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound_VLESSOptions
import io.throneproj.thronem.fmt.SingBoxOptions.Outbound_VMessOptions
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_GRPC
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_HTTP
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_HTTPUPGRADE
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_QUIC
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_WS
import io.throneproj.thronem.fmt.SingBoxOptions.TRANSPORT_XHTTP
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayGRPCOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayHTTPOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayHTTPUpgradeOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayQUICOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayWebsocketOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayXHTTPOptions
import io.throneproj.thronem.fmt.buildHeader
import io.throneproj.thronem.fmt.buildSingBoxMux
import io.throneproj.thronem.fmt.http.HttpBean
import io.throneproj.thronem.fmt.listable
import io.throneproj.thronem.fmt.parseBoxOutbound
import io.throneproj.thronem.fmt.parseBoxTLS
import io.throneproj.thronem.fmt.parseHeader
import io.throneproj.thronem.fmt.toECHOneLine
import io.throneproj.thronem.fmt.toECHPem
import io.throneproj.thronem.fmt.trojan.TrojanBean
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.Logs
import io.throneproj.thronem.ktx.b64Decode
import io.throneproj.thronem.ktx.b64DecodeToString
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.kxs
import io.throneproj.thronem.ktx.listByLineOrComma
import io.throneproj.thronem.ktx.queryParameterNotBlank
import io.throneproj.thronem.ktx.readableMessage
import io.throneproj.thronem.ktx.toJsonObjectKxs
import io.throneproj.thronem.ktx.URL
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

private const val DEFAULT_WS_MAX_EARLY_DATA = 2048
private const val DEFAULT_WS_EARLY_DATA_HEADER = "Sec-WebSocket-Protocol"

/**
 * A legacy but still be used widely and be updated continually format.
 * @see <a href="https://github.com/2dust/v2rayN/wiki/Description-of-VMess-share-link">Description of VMess share link</a>
 */
@Serializable
data class V2rayNVMessShare(
    var v: String = "",
    var ps: String = "",
    var add: String = "",
    var port: JsonPrimitive = JsonPrimitive(""),
    var id: String = "",
    var aid: JsonPrimitive = JsonPrimitive("0"),
    var scy: String = "",
    var net: String = "",
    var type: String = "",
    var host: String = "",
    var path: String = "",
    var tls: String = "",
    var sni: String = "",
    var alpn: String = "",
    var fp: String = "",
    var insecure: String = "",
)

fun StandardV2RayBean.setTLS(boolean: Boolean) {
    security = if (boolean) "tls" else ""
}

fun StandardV2RayBean.shouldMux(): Boolean = serverMux && when (v2rayTransport) {
    "http" -> isTLS
    // xhttp carries its own XMUX connection pool; stream multiplexing on top is
    // not a supported combination (Xray rejects it too).
    "quic", "grpc", "xhttp" -> false
    else -> true
}

fun parseV2Ray(rawUrl: String): StandardV2RayBean {
    // Try parse stupid formats first

    if (!rawUrl.contains("?")) {
        try {
            return parseV2RayN(rawUrl)
        } catch (e: Exception) {
            Logs.i("try v2rayN: " + e.readableMessage)
        }
    }

    val url = parseURL(rawUrl)
    val bean = if (url.scheme == "vmess") {
        VMessBean()
    } else {
        VLESSBean()
    }
    bean.parseDuckSoft(url)

    return bean
}

private fun StandardV2RayBean.parseDuckSoftTlsQueries(url: URL) {
    sni = url.queryParameterNotBlank("sni") ?: url.queryParameterNotBlank("host") ?: ""
    alpn = url.queryParameter("alpn")
    certificates = url.queryParameter("cert")
    realityPublicKey = url.queryParameter("pbk")
    realityShortID = url.queryParameter("sid")

    url.queryParameterNotBlank("ech")?.let {
        ech = true

        if (it.contains("://")) {
            echQueryServerName = it.substringBefore("://", "").substringBefore("+", "")
        } else runCatching {
            if (it.b64Decode().isNotEmpty()) {
                echConfig = it.toECHPem()
            }
        }
    }
}

// https://github.com/XTLS/Xray-core/discussions/716
fun StandardV2RayBean.parseDuckSoft(url: URL) {
    serverAddress = url.host
    serverPort = url.ports.toIntOrNull() ?: 443
    name = url.fragment

    if (this is TrojanBean) {
        password = url.username
    } else {
        uuid = url.username
    }

    v2rayTransport = url.queryParameter("type")
    if (v2rayTransport == "h2" || url.queryParameter("headerType") == "http") {
        v2rayTransport = "http"
    }

    security = url.queryParameter("security")
    when (security) {
        "tls", "reality" -> {
            security = "tls"
            parseDuckSoftTlsQueries(url)
        }

        "" -> if (this is TrojanBean) {
            // The end users who use edge tunnel requires using VLESS without TLS.
            // Fortunately, they didn't pollute trojan's ecosystem.
            // And this standard force trojan's link to use TLS.
            // https://github.com/p4gefau1t/trojan-go/issues/132
            security = "tls"
            parseDuckSoftTlsQueries(url)
        }
    }

    when (v2rayTransport) {
        "", "tcp" -> {}

        "http" -> {
            host = url.queryParameter("host")
            path = url.queryParameter("path")
        }

        "ws" -> {
            host = url.queryParameter("host")
            path = url.queryParameter("path")
            url.queryParameterNotBlank("ed")?.let { ed ->
                wsMaxEarlyData = ed.toIntOrNull() ?: DEFAULT_WS_MAX_EARLY_DATA
                earlyDataHeaderName = url.queryParameterNotBlank("eh")
                    ?: DEFAULT_WS_EARLY_DATA_HEADER
            }
        }

        "grpc" -> {
            path = url.queryParameter("serviceName")
        }

        "httpupgrade" -> {
            host = url.queryParameter("host")
            path = url.queryParameter("path")
        }

        "xhttp" -> url.intoXhttp(this)
    }

    when (this) {
        is VMessBean -> {
            encryption = url.queryParameter("encryption")
        }

        is VLESSBean -> {
            encryption = url.queryParameter("encryption")
            flow = url.queryParameterNotBlank("flow")?.removeSuffix("-udp443").orEmpty()
        }
    }

    utlsFingerprint = url.queryParameter("fp")
}

// SagerNet's
// Do not support some format and then throw exception
fun parseV2RayN(link: String): VMessBean {
    val result = link.substringAfter("vmess://").b64DecodeToString()
    if (result.contains("= vmess")) {
        return parseCsvVMess(result)
    }
    val bean = VMessBean()
    val vmessData = kxs.decodeFromString<V2rayNVMessShare>(result)

    // Although V2rayNVMessShare fields are non-null, decoding may still yield empty values
    @Suppress("UselessCallOnNotNull")
    if (vmessData.add.isNullOrEmpty()
        || vmessData.port.content.isBlank()
        || vmessData.id.isNullOrBlank()
        || vmessData.net.isNullOrBlank()
    ) {
        throw Exception("invalid VmessQRCode")
    }

    bean.name = vmessData.ps
    bean.serverAddress = vmessData.add
    bean.serverPort = vmessData.port.content.toIntOrNull() ?: 10086
    bean.encryption = vmessData.scy
    bean.uuid = vmessData.id
    bean.alterId = vmessData.aid.content.toIntOrNull() ?: 0
    bean.v2rayTransport = vmessData.net
    bean.host = vmessData.host
    bean.path = vmessData.path
    val headerType = vmessData.type

    when (bean.v2rayTransport) {
        "", "tcp" -> {
            if (headerType == "http") {
                bean.v2rayTransport = "http"
            }
        }
    }
    when (vmessData.tls) {
        "tls", "reality" -> {
            bean.security = "tls"
            bean.sni = vmessData.sni
            if (bean.sni.isBlank()) bean.sni = bean.host
            bean.alpn = vmessData.alpn
            bean.utlsFingerprint = vmessData.fp
            bean.allowInsecure = vmessData.insecure == "1"
        }
    }

    return bean
}

private fun parseCsvVMess(csv: String): VMessBean {

    val args = csv.split(",")

    val bean = VMessBean()

    bean.serverAddress = args[1]
    bean.serverPort = args[2].toInt()
    bean.encryption = args[3]
    bean.uuid = args[4].replace("\"", "")

    args.subList(5, args.size).forEach {

        when {
            it == "over-tls=true" -> bean.security = "tls"
            it.startsWith("tls-host=") -> bean.host = it.substringAfter("=")
            it.startsWith("obfs=") -> bean.v2rayTransport = it.substringAfter("=")
            it.startsWith("obfs-path=") || it.contains("Host:") -> {
                runCatching {
                    bean.path = it.substringAfter("obfs-path=\"").substringBefore("\"obfs")
                }
                runCatching {
                    bean.host = it.substringAfter("Host:").substringBefore("[")
                }

            }

        }

    }

    return bean

}

fun StandardV2RayBean.toUriVMessVLESSTrojan(): String {

    var isTrojan = false
    var isVMess = false
    var isVLESS = false
    val protocol = when (this) {
        is TrojanBean -> {
            isTrojan = true
            "trojan"
        }

        is VMessBean -> {
            isVMess = true
            "vmess"
        }

        is VLESSBean -> {
            isVLESS = true
            "vless"
        }

        else -> error("impossible")
    }

    // ducksoft fmt
    val builder = newURL(protocol).apply {
        username = if (isTrojan) {
            (this@toUriVMessVLESSTrojan as TrojanBean).password
        } else {
            uuid
        }
        host = serverAddress
        ports = serverPort.toString()
        // In standard 4.2.1:
        // > The current value must be one of tcp, kcp, ws, http, grpc, httpupgrade, xhttp,
        val transport = v2rayTransport.blankAsNull() ?: "tcp"
        addQueryParameter("type", transport)
    }

    if (!isTrojan) {
        if (isVLESS) {
            this as VLESSBean
            builder.addQueryParameter("flow", flow)
            builder.addQueryParameter("encryption", encryption)
        } else {
            this as VMessBean
            builder.addQueryParameter("encryption", encryption)
        }
    }

    when (v2rayTransport) {
        "", "tcp" -> {}
        "ws", "http", "httpupgrade", "xhttp" -> {
            if (host.isNotBlank()) {
                builder.addQueryParameter("host", host)
            }
            if (path.isNotBlank()) {
                builder.addQueryParameter("path", path)
            }
            if (v2rayTransport == "xhttp") {
                writeXhttpQuery(builder)
            } else if (v2rayTransport == "ws") {
                if (wsMaxEarlyData > 0) {
                    builder.setQueryParameter("ed", "$wsMaxEarlyData")
                    if (earlyDataHeaderName.isNotBlank()) {
                        builder.setQueryParameter("eh", earlyDataHeaderName)
                    }
                }
            } else if (v2rayTransport == "http" && !isTLS) {
                builder.setQueryParameter("type", "tcp")
                builder.addQueryParameter("headerType", "http")
            }
        }

        "grpc" -> {
            if (path.isNotBlank()) {
                builder.setQueryParameter("serviceName", path)
            }
        }
    }

    if (security.isNotBlank() && security != "none") {
        builder.addQueryParameter("security", security)
        when (security) {
            "tls" -> {
                if (sni.isNotBlank()) {
                    builder.addQueryParameter("sni", sni)
                }
                if (alpn.isNotBlank()) {
                    builder.addQueryParameter("alpn", alpn.replace("\n", ","))
                }
                if (certificates.isNotBlank()) {
                    builder.addQueryParameter("cert", certificates)
                }
                if (allowInsecure) {
                    builder.addQueryParameter("allowInsecure", "1")
                }
                if (utlsFingerprint.isNotBlank()) {
                    builder.addQueryParameter("fp", utlsFingerprint)
                }
                if (realityPublicKey.isNotBlank()) {
                    builder.setQueryParameter("security", "reality")
                    builder.addQueryParameter("pbk", realityPublicKey)
                    builder.addQueryParameter("sid", realityShortID)
                }
                // Xray requires a DNS server in ECH field, which is coupling.
                // We don't set a hard-coded DNS server here. 😅
                if (ech) echConfig.blankAsNull()?.let {
                    builder.setQueryParameter("ech", it.toECHOneLine())
                }
            }
        }
    }

    if (name.isNotBlank()) {
        builder.fragment = name
    }

    return builder.string
}

fun buildSingBoxOutboundStreamSettings(bean: StandardV2RayBean): V2RayTransportOptions? {
    when (bean.v2rayTransport) {
        "", "tcp" -> {
            return null
        }

        "ws" -> {
            return V2RayTransportOptions_V2RayWebsocketOptions().apply {
                type = TRANSPORT_WS
                headers = bean.headers.blankAsNull()?.let(::buildHeader)?.toMutableMap()
                    ?: mutableMapOf()

                if (bean.host.isNotBlank()) {
                    headers!!["Host"] = bean.host.listByLineOrComma().toMutableList()
                }

                runCatching {
                    parseURL(bean.path)
                }.onSuccess { pathURL ->
                    pathURL.queryParameterNotBlank("ed")?.toIntOrNull()?.let { maxEarlyData ->
                        max_early_data = maxEarlyData
                        pathURL.removeQueryParameter("ed")
                    }
                    pathURL.queryParameterNotBlank("eh")?.let { headerName ->
                        early_data_header_name = headerName
                        pathURL.removeQueryParameter("eh")
                    }
                    path = pathURL.string
                }.onFailure {
                    path = bean.path
                }

                if (bean.wsMaxEarlyData > 0) {
                    max_early_data = bean.wsMaxEarlyData
                }

                if (bean.earlyDataHeaderName.isNotBlank()) {
                    early_data_header_name = bean.earlyDataHeaderName
                }
            }
        }

        "http" -> {
            return V2RayTransportOptions_V2RayHTTPOptions().apply {
                type = TRANSPORT_HTTP
                if (!bean.isTLS) method = "GET" // v2ray tcp header
                if (bean.host.isNotBlank()) {
                    host = bean.host.listByLineOrComma().toMutableList()
                }
                path = bean.path.takeIf { it.isNotBlank() } ?: "/"

                headers = bean.headers.blankAsNull()?.let(::buildHeader)?.toMutableMap()
            }
        }

        "quic" -> {
            return V2RayTransportOptions().apply {
                type = TRANSPORT_QUIC
            }
        }

        "grpc" -> {
            return V2RayTransportOptions_V2RayGRPCOptions().apply {
                type = TRANSPORT_GRPC
                service_name = bean.path
            }
        }

        "httpupgrade" -> {
            return V2RayTransportOptions_V2RayHTTPUpgradeOptions().apply {
                type = TRANSPORT_HTTPUPGRADE
                host = bean.host.listByLineOrComma().firstOrNull()
                path = bean.path

                headers = bean.headers.blankAsNull()?.let(::buildHeader)?.toMutableMap()
            }
        }

        "xhttp" -> {
            return buildSingBoxXhttpTransport(bean)
        }
    }

    return null
}

fun buildSingBoxOutboundTLS(bean: StandardV2RayBean): OutboundTLSOptions? {
    if (bean.security != "tls") return null
    return OutboundTLSOptions().apply {
        enabled = true
        if (bean.allowInsecure) insecure = true
        if (bean.disableSNI) disable_sni = true
        if (bean.sni.isNotBlank()) server_name = bean.sni
        alpn = bean.alpn.blankAsNull()?.listByLineOrComma()?.toMutableList()
        certificate = bean.certificates.blankAsNull()?.lines()?.toMutableList()
        certificate_public_key_sha256 = bean.certPublicKeySha256
            .blankAsNull()
            ?.lines()
            ?.toMutableList()
        client_certificate = bean.clientCert.blankAsNull()?.lines()?.toMutableList()
        client_key = bean.clientKey.blankAsNull()?.lines()?.toMutableList()
        if (bean.fragment) {
            fragment = true
            fragment_fallback_delay = bean.fragmentFallbackDelay.blankAsNull()
        }
        if (bean.recordFragment) record_fragment = true
        bean.tlsSpoof.blankAsNull()?.let {
            spoof = it
            spoof_method = bean.tlsSpoofMethod.blankAsNull()
        }
        var fingerprint = bean.utlsFingerprint
        if (bean.realityPublicKey.isNotBlank()) {
            reality = OutboundRealityOptions().apply {
                enabled = true
                public_key = bean.realityPublicKey
                short_id = bean.realityShortID
            }
            if (fingerprint.isBlank()) fingerprint = "chrome"
        }
        if (fingerprint.isNotBlank()) {
            utls = OutboundUTLSOptions().apply {
                enabled = true
                this.fingerprint = fingerprint
            }
        }
        if (bean.ech) {
            ech = OutboundECHOptions().apply {
                enabled = true
                config = bean.echConfig.blankAsNull()?.lines()?.toMutableList()
                query_server_name = bean.echQueryServerName.blankAsNull()
            }
        }
    }
}

suspend fun buildSingBoxOutboundStandardV2RayBean(bean: StandardV2RayBean): Outbound = when (bean) {
    is HttpBean -> Outbound_HTTPOptions().apply {
        type = SingBoxOptions.TYPE_HTTP
        server = bean.serverAddress
        server_port = bean.serverPort
        username = bean.username
        password = bean.password
        path = bean.path
        tls = buildSingBoxOutboundTLS(bean)

        headers = bean.headers.blankAsNull()?.let(::buildHeader)?.toMutableMap()
    }

    is VMessBean -> Outbound_VMessOptions().apply {
        type = SingBoxOptions.TYPE_VMESS
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        alter_id = bean.alterId
        security = bean.encryption.takeIf { it.isNotBlank() } ?: "auto"
        packet_encoding = when (bean.packetEncoding) {
            StandardV2RayBean.PACKET_ENCODING_PACKETADDR -> "packetaddr"
            StandardV2RayBean.PACKET_ENCODING_XUDP -> "xudp"
            else -> null
        }
        tls = buildSingBoxOutboundTLS(bean)
        transport = buildSingBoxOutboundStreamSettings(bean)?.toJsonObjectKxs()
        if (bean.shouldMux()) multiplex = buildSingBoxMux(bean)

        global_padding = true
        authenticated_length = bean.authenticatedLength
    }

    is VLESSBean -> Outbound_VLESSOptions().apply {
        type = SingBoxOptions.TYPE_VLESS
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        flow = bean.flow.blankAsNull()
        encryption = bean.encryption.takeUnless { it.isBlank() || it == "none" }
        packet_encoding = when (bean.packetEncoding) {
            StandardV2RayBean.PACKET_ENCODING_PACKETADDR -> "packetaddr"
            StandardV2RayBean.PACKET_ENCODING_XUDP -> "xudp"
            else -> null
        }
        tls = buildSingBoxOutboundTLS(bean)
        transport = buildSingBoxOutboundStreamSettings(bean)?.toJsonObjectKxs()
        if (bean.shouldMux()) multiplex = buildSingBoxMux(bean)
    }

    is TrojanBean -> Outbound_TrojanOptions().apply {
        type = SingBoxOptions.TYPE_TROJAN
        server = bean.serverAddress
        server_port = bean.serverPort
        password = bean.password
        tls = buildSingBoxOutboundTLS(bean)
        transport = buildSingBoxOutboundStreamSettings(bean)?.toJsonObjectKxs()
        if (bean.shouldMux()) multiplex = buildSingBoxMux(bean)
    }

    else -> throw IllegalStateException()
}

fun parseStandardV2RayOutbound(json: JSONMap): StandardV2RayBean {
    var v2ray: StandardV2RayBean? = null
    var vmess: VMessBean? = null
    var vless: VLESSBean? = null
    var trojan: TrojanBean? = null
    val bean = when (json["type"]!!.toString()) {
        SingBoxOptions.TYPE_VMESS -> VMessBean().also {
            v2ray = it
            vmess = it
        }

        SingBoxOptions.TYPE_VLESS -> VLESSBean().also {
            v2ray = it
            vless = it
        }

        SingBoxOptions.TYPE_TROJAN -> TrojanBean().also {
            trojan = it
        }

        else -> throw IllegalStateException()
    }

    bean.parseBoxOutbound(json) { key, value ->
        when (key) {
            "alter_id" -> value.toString().toIntOrNull()?.let {
                vmess?.alterId = it
            }

            "authenticated_length" -> vmess?.authenticatedLength = value.toString().toBoolean()
            "uuid" -> v2ray?.uuid = value.toString()
            "flow" -> vless?.flow = value.toString().removeSuffix("-udp443")
            "encryption" -> vless?.encryption = value.toString()
            "password" -> trojan?.password = value.toString()
            "security" -> vmess?.security = value.toString()
            "packet_encoding" -> v2ray?.packetEncoding = when (value.toString()) {
                "packetaddr" -> StandardV2RayBean.PACKET_ENCODING_PACKETADDR
                "xudp" -> StandardV2RayBean.PACKET_ENCODING_XUDP
                else -> StandardV2RayBean.PACKET_ENCODING_NONE
            }

            "transport" -> {
                val transport = parseTransport(value as JSONMap) ?: return@parseBoxOutbound

                bean.v2rayTransport = transport.type.orEmpty()
                when (transport) {
                    is V2RayTransportOptions_V2RayWebsocketOptions -> {
                        bean.host = transport.headers?.get("host")?.joinToString("\n")?.also {
                            transport.headers!!.remove("host")
                        }.orEmpty()
                        transport.headers?.map { entry ->
                            entry.key + ":" + entry.value.joinToString(",")
                        }?.joinToString("\n")?.let {
                            bean.headers = it
                        }
                        bean.path = transport.path.orEmpty()
                        bean.wsMaxEarlyData = transport.max_early_data ?: 0
                        bean.earlyDataHeaderName = transport.early_data_header_name.orEmpty()
                    }

                    is V2RayTransportOptions_V2RayHTTPOptions -> {
                        bean.host = transport.host?.joinToString("\n").orEmpty()
                        bean.path = transport.path.orEmpty()
                        bean.headers = transport.headers?.let {
                            parseHeader(it).map { entry ->
                                entry.key + ":" + entry.value.joinToString(",")
                            }.joinToString("\n")
                        }.orEmpty()
                    }

                    is V2RayTransportOptions_V2RayQUICOptions -> {}

                    is V2RayTransportOptions_V2RayGRPCOptions -> {
                        bean.path = transport.service_name.orEmpty()
                    }

                    is V2RayTransportOptions_V2RayHTTPUpgradeOptions -> {
                        bean.host = transport.host.orEmpty()
                        bean.path = transport.path.orEmpty()
                        bean.headers = transport.headers?.let {
                            parseHeader(it).map { entry ->
                                entry.key + ":" + entry.value.joinToString(",")
                            }.joinToString("\n")
                        }.orEmpty()
                    }

                    is V2RayTransportOptions_V2RayXHTTPOptions -> {
                        bean.applyXhttpJson(transport)
                    }
                }
            }

            "tls" -> {
                val tls = parseBoxTLS(value as JSONMap)

                bean.setTLS(tls.enabled == true)
                bean.sni = tls.server_name.orEmpty()
                bean.allowInsecure = tls.insecure == true
                bean.disableSNI = tls.disable_sni == true
                bean.alpn = tls.alpn?.joinToString(",").orEmpty()
                bean.certificates = tls.certificate?.joinToString("\n").orEmpty()
                bean.certPublicKeySha256 =
                    tls.certificate_public_key_sha256?.joinToString("\n").orEmpty()
                bean.clientCert = tls.client_certificate?.joinToString("\n").orEmpty()
                bean.clientKey = tls.client_key?.joinToString("\n").orEmpty()
                bean.utlsFingerprint = tls.utls?.fingerprint.orEmpty()
                bean.fragment = tls.fragment == true
                bean.fragmentFallbackDelay = tls.fragment_fallback_delay.orEmpty()
                bean.recordFragment = tls.record_fragment == true
                bean.tlsSpoof = tls.spoof.orEmpty()
                bean.tlsSpoofMethod = tls.spoof_method.orEmpty()
                tls.ech?.let {
                    bean.ech = it.enabled == true
                    bean.echConfig = it.config?.joinToString("\n").orEmpty()
                    bean.echQueryServerName = it.query_server_name.orEmpty()
                }
                tls.reality?.let {
                    bean.realityPublicKey = it.public_key.orEmpty()
                    bean.realityShortID = it.short_id.orEmpty()
                }
            }
        }
    }

    return bean
}

fun parseTransport(json: JSONMap): V2RayTransportOptions? = when (json["type"]?.toString()) {
    TRANSPORT_WS -> V2RayTransportOptions_V2RayWebsocketOptions().apply {
        type = TRANSPORT_WS
        headers = (json["headers"] as? JSONMap)?.let {
            parseHeader(it).toMutableMap()
        } ?: mutableMapOf()
        path = json["path"]?.toString()
        max_early_data = json["max_early_data"]?.toString()?.toIntOrNull()
        early_data_header_name = json["early_data_header_name"]?.toString()
    }

    TRANSPORT_HTTP -> V2RayTransportOptions_V2RayHTTPOptions().apply {
        type = TRANSPORT_HTTP
        host = listable<String>(json["host"])
        path = json["path"]?.toString()
        headers = (json["headers"] as? JSONMap)?.let {
            parseHeader(it).toMutableMap()
        } ?: mutableMapOf()
    }

    TRANSPORT_QUIC -> V2RayTransportOptions_V2RayQUICOptions().apply {
        type = TRANSPORT_QUIC
    }

    TRANSPORT_GRPC -> V2RayTransportOptions_V2RayGRPCOptions().apply {
        type = TRANSPORT_GRPC
        service_name = json["service_name"]?.toString()
    }

    TRANSPORT_HTTPUPGRADE -> V2RayTransportOptions_V2RayHTTPUpgradeOptions().apply {
        type = TRANSPORT_HTTPUPGRADE
        host = json["host"]?.toString()
        path = json["path"]?.toString()
        headers = (json["headers"] as? JSONMap)?.let {
            parseHeader(it).toMutableMap()
        } ?: mutableMapOf()
    }

    TRANSPORT_XHTTP -> parseXhttpJson(json)

    else -> null
}
