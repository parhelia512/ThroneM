package io.throneproj.thronem.fmt

import androidx.room.TypeConverter
import io.throneproj.thronem.database.SubscriptionBean
import io.throneproj.thronem.fmt.anytls.AnyTLSBean
import io.throneproj.thronem.fmt.config.ConfigBean
import io.throneproj.thronem.fmt.direct.DirectBean
import io.throneproj.thronem.fmt.http.HttpBean
import io.throneproj.thronem.fmt.hysteria.HysteriaBean
import io.throneproj.thronem.fmt.internal.ChainBean
import io.throneproj.thronem.fmt.internal.ProxySetBean
import io.throneproj.thronem.fmt.juicity.JuicityBean
import io.throneproj.thronem.fmt.naive.NaiveBean
import io.throneproj.thronem.fmt.masque.MasqueBean
import io.throneproj.thronem.fmt.openconnect.OpenConnectBean
import io.throneproj.thronem.fmt.openvpn.OpenVPNBean
import io.throneproj.thronem.fmt.shadowsocks.ShadowsocksBean
import io.throneproj.thronem.fmt.shadowtls.ShadowTLSBean
import io.throneproj.thronem.fmt.snell.SnellBean
import io.throneproj.thronem.fmt.socks.SOCKSBean
import io.throneproj.thronem.fmt.ssh.SSHBean
import io.throneproj.thronem.fmt.trojan.TrojanBean
import io.throneproj.thronem.fmt.tuic.TuicBean
import io.throneproj.thronem.fmt.v2ray.VLESSBean
import io.throneproj.thronem.fmt.v2ray.VMessBean
import io.throneproj.thronem.fmt.wireguard.WireGuardBean
import io.throneproj.thronem.io.BinaryFormatException
import io.throneproj.thronem.io.BinaryInput
import io.throneproj.thronem.io.BinaryOutput
import io.throneproj.thronem.ktx.Logs

/** Turns the beans stored in Room `BLOB` columns into bytes and back. */
class BeanConverters {

    companion object {
        private val NULL = ByteArray(0)

        @JvmStatic
        fun serialize(bean: Serializable?): ByteArray {
            if (bean == null) return NULL
            val output = BinaryOutput()
            bean.serializeToBuffer(output)
            return output.toByteArray()
        }

        @TypeConverter
        @JvmStatic
        fun serializeForRoom(bean: Serializable?): ByteArray? = serialize(bean)

        @JvmStatic
        fun <T : Serializable> deserialize(bean: T, bytes: ByteArray?): T {
            if (bytes == null) return bean
            try {
                bean.deserializeFromBuffer(BinaryInput(bytes))
            } catch (e: BinaryFormatException) {
                // A payload written by an older thronem version simply ends early; keep what it held.
                Logs.w(e)
            }
            bean.initializeDefaultValues()
            return bean
        }

        @TypeConverter
        @JvmStatic
        fun socksDeserialize(bytes: ByteArray?): SOCKSBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(SOCKSBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun httpDeserialize(bytes: ByteArray?): HttpBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(HttpBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun shadowsocksDeserialize(bytes: ByteArray?): ShadowsocksBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(ShadowsocksBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun snellDeserialize(bytes: ByteArray?): SnellBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(SnellBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun configDeserialize(bytes: ByteArray?): ConfigBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(ConfigBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun vmessDeserialize(bytes: ByteArray?): VMessBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(VMessBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun vlessDeserialize(bytes: ByteArray?): VLESSBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(VLESSBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun trojanDeserialize(bytes: ByteArray?): TrojanBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(TrojanBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun naiveDeserialize(bytes: ByteArray?): NaiveBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(NaiveBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun hysteriaDeserialize(bytes: ByteArray?): HysteriaBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(HysteriaBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun sshDeserialize(bytes: ByteArray?): SSHBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(SSHBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun wireguardDeserialize(bytes: ByteArray?): WireGuardBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(WireGuardBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun openConnectDeserialize(bytes: ByteArray?): OpenConnectBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(OpenConnectBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun openVPNDeserialize(bytes: ByteArray?): OpenVPNBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(OpenVPNBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun masqueDeserialize(bytes: ByteArray?): MasqueBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(MasqueBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun tuicDeserialize(bytes: ByteArray?): TuicBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(TuicBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun juicityDeserialize(bytes: ByteArray?): JuicityBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(JuicityBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun directDeserialize(bytes: ByteArray?): DirectBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(DirectBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun anyTLSDeserialize(bytes: ByteArray?): AnyTLSBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(AnyTLSBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun shadowTLSDeserialize(bytes: ByteArray?): ShadowTLSBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(ShadowTLSBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun proxySetDeserialize(bytes: ByteArray?): ProxySetBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(ProxySetBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun chainDeserialize(bytes: ByteArray?): ChainBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(ChainBean(), bytes)
        }

        @TypeConverter
        @JvmStatic
        fun subscriptionDeserialize(bytes: ByteArray?): SubscriptionBean? {
            if (bytes?.isNotEmpty() != true) return null
            return deserialize(SubscriptionBean(), bytes)
        }
    }
}
