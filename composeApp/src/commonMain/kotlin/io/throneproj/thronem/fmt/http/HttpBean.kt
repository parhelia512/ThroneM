package io.throneproj.thronem.fmt.http

import kotlinx.serialization.Serializable as KxsSerializable
import io.throneproj.thronem.fmt.BeanConverters
import io.throneproj.thronem.fmt.ValidateResult
import io.throneproj.thronem.fmt.v2ray.StandardV2RayBean
import io.throneproj.thronem.io.BinaryInput
import io.throneproj.thronem.io.BinaryOutput

@KxsSerializable
class HttpBean : StandardV2RayBean() {

    companion object {
        @JvmField
        val CREATOR = object : CREATOR<HttpBean>() {
            override fun newInstance(): HttpBean {
                return HttpBean()
            }

            override fun newArray(size: Int): Array<HttpBean?> {
                return arrayOfNulls(size)
            }
        }
    }

    var username: String = ""
    var password: String = ""

    override fun isInsecure(): ValidateResult {
        val result = super.isInsecure()
        if (shouldReturnFromInsecureCheck(result)) return result

        return validateTLSSettings(requireTLS = true, warnAllowInsecure = false)
    }

    override fun serialize(output: BinaryOutput) {
        output.writeInt(3)

        // version 0
        super.serialize(output)
        output.writeString(username)
        output.writeString(password)

        // version 1
        output.writeString(host)
        output.writeString(path)
        output.writeString(headers)
    }

    override fun deserialize(input: BinaryInput) {
        val version = input.readInt()
        super.deserialize(input)
        username = input.readString()
        password = input.readString()
        if (version >= 1) {
            host = input.readString()
            path = input.readString()
            headers = input.readString()
        }
        if (version == 2) {
            input.readBoolean() // removed UDP over TCP flag
        }
    }

    override fun clone(): HttpBean {
        return BeanConverters.deserialize(HttpBean(), BeanConverters.serialize(this))
    }

    override val defaultPort get() = if (isTLS) 443 else 80
}
