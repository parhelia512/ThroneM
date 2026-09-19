package io.throneproj.thronem.fmt.trojan

import io.throneproj.thronem.fmt.v2ray.parseDuckSoft
import io.throneproj.thronem.ktx.parseBoolean
import io.throneproj.thronem.ktx.queryParameterNotBlank
import io.throneproj.thronem.ktx.newURL
import io.throneproj.thronem.ktx.parseURL

fun parseTrojan(link: String): TrojanBean {
    val url = parseURL(link)
    return TrojanBean().apply {
        parseDuckSoft(url)
        allowInsecure = url.parseBoolean("allowInsecure")
        url.queryParameterNotBlank("peer")?.let {
            sni = it
        }
    }

}
