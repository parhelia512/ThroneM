package io.throneproj.thronem.fmt.masque

import kotlinx.serialization.Serializable as KxsSerializable
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.BeanConverters
import io.throneproj.thronem.io.BinaryInput
import io.throneproj.thronem.io.BinaryOutput

/**
 * Cloudflare WARP over MASQUE (CONNECT-IP), sing-box-lx outbound type
 * "masque". Key material is ECDSA P-256 DER, base64-encoded: [privateKey] is
 * the SEC1 `ECPrivateKey` of this device, [publicKey] the PKIX/SPKI public key
 * of the pinned Cloudflare server.
 */
@KxsSerializable
class MasqueBean : AbstractBean() {

    companion object {
        @JvmField
        val CREATOR = object : CREATOR<MasqueBean>() {
            override fun newInstance(): MasqueBean {
                return MasqueBean()
            }

            override fun newArray(size: Int): Array<MasqueBean?> {
                return arrayOfNulls(size)
            }
        }
    }

    var privateKey: String = ""
    var publicKey: String = ""
    var localIp: String = ""
    var localIpv6: String = ""

    /** HTTP version carrying CONNECT-IP: "auto" / "h3" / "h2". */
    var vhttp: String = "auto"
    var sni: String = ""
    var mtu: Int = 1280

    /** Go duration ("5m"); blank = core default. */
    var idleTimeout: String = ""

    /** QUIC keep-alive period ("30s"); blank = core default. */
    var keepAlive: String = ""

    override fun initializeDefaultValues() {
        super.initializeDefaultValues()
        if (vhttp.isBlank()) vhttp = "auto"
        if (mtu <= 0) mtu = 1280
    }

    override fun serialize(output: BinaryOutput) {
        output.writeInt(1)
        super.serialize(output)
        output.writeString(privateKey)
        output.writeString(publicKey)
        output.writeString(localIp)
        output.writeString(localIpv6)
        output.writeString(vhttp)
        output.writeString(sni)
        output.writeInt(mtu)
        output.writeString(idleTimeout)
        output.writeString(keepAlive)
    }

    override fun deserialize(input: BinaryInput) {
        val version = input.readInt()
        super.deserialize(input)
        privateKey = input.readString()
        publicKey = input.readString()
        localIp = input.readString()
        localIpv6 = input.readString()
        vhttp = input.readString()
        sni = input.readString()
        mtu = input.readInt()
        idleTimeout = input.readString()
        keepAlive = input.readString()
    }

    override val canTCPing = false

    override fun clone(): MasqueBean {
        return BeanConverters.deserialize(MasqueBean(), BeanConverters.serialize(this))
    }

    override val defaultPort = 443
}
