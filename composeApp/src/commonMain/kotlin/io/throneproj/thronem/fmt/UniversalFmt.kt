package io.throneproj.thronem.fmt

import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.database.ProxyGroup
import io.throneproj.thronem.ktx.b64Decode
import io.throneproj.thronem.ktx.b64EncodeUrlSafe
import io.throneproj.thronem.ktx.zlibCompress
import io.throneproj.thronem.ktx.zlibDecompress

fun parseUniversal(link: String): AbstractBean {
    return if (link.contains("?")) {
        val type = link.substringAfter("thronem://").substringBefore("?")
        ProxyEntity(type = TypeMap[type] ?: error("Type $type not found")).apply {
            putByteArray(link.substringAfter("?").b64Decode().zlibDecompress())
        }.requireBean()
    } else {
        val type = link.substringAfter("thronem://").substringBefore(":")
        ProxyEntity(type = TypeMap[type] ?: error("Type $type not found")).apply {
            putByteArray(link.substringAfter(":").substringAfter(":").b64Decode())
        }.requireBean()
    }
}

fun AbstractBean.toUniversalLink(): String {
    var link = "thronem://"
    val type = ProxyEntity().putBean(this).type
    link += TypeMap.reversed[type] ?: error("Type $type not found")
    link += "?"
    link += BeanConverters.serialize(this).zlibCompress(9).b64EncodeUrlSafe()
    return link
}


fun ProxyGroup.toUniversalLink(): String {
    var link = "thronem://subscription?"
    export = true
    link += BeanConverters.serialize(this).zlibCompress(9).b64EncodeUrlSafe()
    export = false
    return link
}
