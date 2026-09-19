package io.throneproj.thronem.fmt.wireguard

import kotlinx.serialization.Serializable as KxsSerializable
import io.throneproj.thronem.fmt.AbstractBean
import io.throneproj.thronem.fmt.BeanConverters
import io.throneproj.thronem.io.BinaryInput
import io.throneproj.thronem.io.BinaryOutput

@KxsSerializable
class WireGuardBean : AbstractBean() {

    companion object {
        @JvmField
        val CREATOR = object : CREATOR<WireGuardBean>() {
            override fun newInstance(): WireGuardBean {
                return WireGuardBean()
            }

            override fun newArray(size: Int): Array<WireGuardBean?> {
                return arrayOfNulls(size)
            }
        }
    }

    var localAddress: String = ""
    var privateKey: String = ""
    var publicKey: String = ""
    var preSharedKey: String = ""
    var mtu: Int = 1420
    var reserved: String = ""

    /**
     * Enable listen if it > 0
     */
    var listenPort: Int = 0
    var persistentKeepaliveInterval: Int = 0

    // AmneziaWG obfuscation (sing-box-lx endpoint root fields). Zero/empty
    // means unset; when any of these is set the bean builds an obfuscated
    // wireguard endpoint (junk packets + magic headers + masquerade sugar).
    var awgJc: Int = 0
    var awgJmin: Int = 0
    var awgJmax: Int = 0
    var awgS1: Int = 0
    var awgS2: Int = 0
    var awgH1: String = ""
    var awgH2: String = ""
    var awgH3: String = ""
    var awgH4: String = ""
    var awgI1: String = ""
    var awgI2: String = ""
    var awgI3: String = ""
    var awgI4: String = ""
    var awgI5: String = ""

    /** WireSock masquerade sugar: id = domain, ip = protocol (quic/dns/stun/sip), ib = browser. */
    var awgId: String = ""
    var awgIp: String = ""
    var awgIb: String = ""

    val hasAwg: Boolean
        get() = awgJc > 0 || awgJmin > 0 || awgJmax > 0 || awgS1 > 0 || awgS2 > 0 ||
            awgH1.isNotBlank() || awgH2.isNotBlank() || awgH3.isNotBlank() || awgH4.isNotBlank() ||
            awgI1.isNotBlank() || awgI2.isNotBlank() || awgI3.isNotBlank() || awgI4.isNotBlank() || awgI5.isNotBlank() ||
            awgId.isNotBlank() || awgIp.isNotBlank() || awgIb.isNotBlank()

    /** Strips every AWG field (back to plain WireGuard). */
    fun clearAwg() {
        awgJc = 0; awgJmin = 0; awgJmax = 0; awgS1 = 0; awgS2 = 0
        awgH1 = ""; awgH2 = ""; awgH3 = ""; awgH4 = ""
        awgI1 = ""; awgI2 = ""; awgI3 = ""; awgI4 = ""; awgI5 = ""
        awgId = ""; awgIp = ""; awgIb = ""
    }

    override fun initializeDefaultValues() {
        super.initializeDefaultValues()
        if (mtu !in 1000..65535) mtu = 1420
    }

    override fun serialize(output: BinaryOutput) {
        output.writeInt(3)
        super.serialize(output)
        output.writeString(localAddress)
        output.writeString(privateKey)
        output.writeString(publicKey)
        output.writeString(preSharedKey)
        output.writeInt(mtu)
        output.writeString(reserved)
        output.writeInt(listenPort)
        output.writeInt(persistentKeepaliveInterval)
        if (hasAwg) {
            output.writeInt(1)
            output.writeInt(awgJc)
            output.writeInt(awgJmin)
            output.writeInt(awgJmax)
            output.writeInt(awgS1)
            output.writeInt(awgS2)
            output.writeString(awgH1)
            output.writeString(awgH2)
            output.writeString(awgH3)
            output.writeString(awgH4)
            output.writeString(awgI1)
            output.writeString(awgI2)
            output.writeString(awgI3)
            output.writeString(awgI4)
            output.writeString(awgI5)
            output.writeString(awgId)
            output.writeString(awgIp)
            output.writeString(awgIb)
        } else {
            output.writeInt(0)
        }
    }

    override fun deserialize(input: BinaryInput) {
        val version = input.readInt()
        super.deserialize(input)
        localAddress = input.readString()
        privateKey = input.readString()
        publicKey = input.readString()
        preSharedKey = input.readString()
        mtu = input.readInt()
        reserved = input.readString()
        if (version >= 1) {
            listenPort = input.readInt()
        }
        if (version >= 2) {
            persistentKeepaliveInterval = input.readInt()
        }
        if (version >= 3 && input.readInt() == 1) {
            awgJc = input.readInt()
            awgJmin = input.readInt()
            awgJmax = input.readInt()
            awgS1 = input.readInt()
            awgS2 = input.readInt()
            awgH1 = input.readString()
            awgH2 = input.readString()
            awgH3 = input.readString()
            awgH4 = input.readString()
            awgI1 = input.readString()
            awgI2 = input.readString()
            awgI3 = input.readString()
            awgI4 = input.readString()
            awgI5 = input.readString()
            awgId = input.readString()
            awgIp = input.readString()
            awgIb = input.readString()
        }
    }

    override val canTCPing = false

    override fun clone(): WireGuardBean {
        return BeanConverters.deserialize(WireGuardBean(), BeanConverters.serialize(this))
    }

    override val defaultPort = 51820
}
