package io.throneproj.thronem.fmt.juicity

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.parseBoolean
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

// https://github.com/juicity/juicity/blob/4af4f68b405a6b86560ebb16963d133a7196af5c/README.md
fun parseJuicity(link: String): JuicityBean {
    val url = parseURL(link)
    return JuicityBean().apply {
        name = url.fragment
        uuid = url.username
        password = url.password
        serverAddress = url.host
        serverPort = url.ports.toIntOrNull() ?: 443

        // url.queryParameter("congestion_control")
        sni = url.queryParameter("sni")
        url.parseBoolean("allow_insecure")
        pinSHA256 = url.queryParameter("pinned_certchain_sha256")
    }
}

fun JuicityBean.toUri(): String {
    return newURL("juicity").apply {
        username = uuid
        password = this@toUri.password
        host = serverAddress
        ports = serverPort.toString()

        addQueryParameter("congestion_control", "bbr")
        if (sni.isNotBlank()) addQueryParameter("sni", sni)
        if (allowInsecure) addQueryParameter("allow_insecure", "1")
        if (pinSHA256.isNotBlank()) addQueryParameter("pinned_certchain_sha256", pinSHA256)
    }.string
}

fun buildSingBoxOutboundJuicityBean(bean: JuicityBean): SingBoxOptions.Outbound_JuicityOptions {
    return SingBoxOptions.Outbound_JuicityOptions().apply {
        type = SingBoxOptions.TYPE_JUICITY
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        password = bean.password
        pin_cert_sha256 = bean.pinSHA256.blankAsNull()
        tls = SingBoxOptions.OutboundTLSOptions().apply {
            enabled = true
            server_name = bean.sni.blankAsNull()
            insecure = bean.allowInsecure || pin_cert_sha256 != null
        }
    }
}
