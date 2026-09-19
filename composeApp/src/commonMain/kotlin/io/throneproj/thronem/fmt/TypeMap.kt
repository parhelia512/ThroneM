package io.throneproj.thronem.fmt

import io.throneproj.thronem.database.ProxyEntity
import io.throneproj.thronem.ktx.reverse

object TypeMap : HashMap<String, Int>() {
    init {
        this["socks"] = ProxyEntity.TYPE_SOCKS
        this["http"] = ProxyEntity.TYPE_HTTP
        this["ss"] = ProxyEntity.TYPE_SS
        this["snell"] = ProxyEntity.TYPE_SNELL
        this["vmess"] = ProxyEntity.TYPE_VMESS
        this["vless"] = ProxyEntity.TYPE_VLESS
        this["trojan"] = ProxyEntity.TYPE_TROJAN
        this["naive"] = ProxyEntity.TYPE_NAIVE
        this["hysteria"] = ProxyEntity.TYPE_HYSTERIA
        this["ssh"] = ProxyEntity.TYPE_SSH
        this["wg"] = ProxyEntity.TYPE_WG
        this["tuic"] = ProxyEntity.TYPE_TUIC
        this["juicity"] = ProxyEntity.TYPE_JUICITY
        this["direct"] = ProxyEntity.TYPE_DIRECT
        this["anytls"] = ProxyEntity.TYPE_ANYTLS
        this["masque"] = ProxyEntity.TYPE_MASQUE
        this["shadowtls"] = ProxyEntity.TYPE_SHADOWTLS
        this["config"] = ProxyEntity.TYPE_CONFIG
    }

    val reversed = reverse()

}
