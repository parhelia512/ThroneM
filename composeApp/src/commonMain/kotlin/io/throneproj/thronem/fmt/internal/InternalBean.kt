package io.throneproj.thronem.fmt.internal

import kotlinx.serialization.Serializable as KxsSerializable
import io.throneproj.thronem.fmt.AbstractBean

@KxsSerializable
abstract class InternalBean : AbstractBean() {

    override fun displayAddress(): String {
        return ""
    }

    override val canICMPing get() = false
    override val canTCPing get() = false
}
