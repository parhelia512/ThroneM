package io.throneproj.thronem.database

import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_ANYTLS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_CHAIN
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_CONFIG
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_DIRECT
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_HTTP
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_HYSTERIA
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_JUICITY
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_MASQUE
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_NAIVE
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_OPENCONNECT
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_OPENVPN
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_PROXY_SET
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_SHADOWTLS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_SNELL
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_SOCKS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_SS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_SSH
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_TROJAN
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_TUIC
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_VLESS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_VMESS
import io.throneproj.thronem.database.ProxyEntity.Companion.TYPE_WG
import io.throneproj.thronem.repository.resolveRepository
import io.throneproj.thronem.resources.*
import kotlinx.coroutines.runBlocking

fun ProxyEntity.displayType(): String = when (type) {
    TYPE_SOCKS -> socksBean!!.protocolName()
    TYPE_HTTP -> if (httpBean!!.isTLS) "HTTPS" else "HTTP"
    TYPE_SS -> "Shadowsocks"
    TYPE_SNELL -> "Snell"
    TYPE_VMESS -> "VMess"
    TYPE_VLESS -> "VLESS"
    TYPE_TROJAN -> "Trojan"
    TYPE_NAIVE -> "Naïve"
    TYPE_HYSTERIA -> "Hysteria" + hysteriaBean!!.protocolVersion
    TYPE_SSH -> "SSH"
    TYPE_WG -> "WireGuard"
    TYPE_OPENCONNECT -> "OpenConnect"
    TYPE_OPENVPN -> "OpenVPN"
    TYPE_MASQUE -> "MASQUE"
    TYPE_TUIC -> "TUIC"
    TYPE_JUICITY -> "Juicity"
    TYPE_SHADOWTLS -> "ShadowTLS"
    TYPE_DIRECT -> "Direct"
    TYPE_ANYTLS -> "AnyTLS"
    TYPE_PROXY_SET -> proxySetBean!!.displayType()
    TYPE_CHAIN -> runBlocking {
        resolveRepository().getString(Res.string.proxy_chain)
    }
    TYPE_CONFIG -> configBean!!.displayType()
    else -> "Undefined type $type"
}
