package io.throneproj.thronem.database

import io.throneproj.thronem.fmt.Serializable
import io.throneproj.thronem.io.BinaryInput
import io.throneproj.thronem.io.BinaryOutput

class SubscriptionBean : Serializable() {

    companion object {
        @JvmField
        val CREATOR = object : CREATOR<SubscriptionBean>() {
            override fun newInstance(): SubscriptionBean {
                return SubscriptionBean()
            }

            override fun newArray(size: Int): Array<SubscriptionBean?> {
                return arrayOfNulls(size)
            }
        }
    }

    var type: Int = 0
    var link: String = ""
    var token: String = ""
    var forceResolve: Boolean = false
    var deduplication: Boolean = false
    var filterNotRegex: String = ""
    var updateWhenConnectedOnly: Boolean = false
    var customUserAgent: String = ""
    var autoUpdate: Boolean = false
    var autoUpdateDelay: Int = 1440
    var lastUpdated: Int = 0

    // SIP008
    var bytesUsed: Long = 0L
    var bytesRemaining: Long = 0L // Also for OOC

    // Open Online Config
    var username: String = ""
    var expiryDate: Long = 0L

    override fun serializeToBuffer(output: BinaryOutput) {
        // 5 carried ageIdentity, dropped with the age feature; still consumed
        // when reading version 5 blobs.
        output.writeInt(6)

        output.writeInt(type)
        output.writeString(link)

        output.writeBoolean(forceResolve)
        output.writeBoolean(deduplication)
        output.writeBoolean(updateWhenConnectedOnly)
        output.writeString(customUserAgent)
        output.writeBoolean(autoUpdate)
        output.writeInt(autoUpdateDelay)
        output.writeInt(lastUpdated)
        output.writeLong(expiryDate)
        output.writeLong(bytesUsed)
        output.writeLong(bytesRemaining)
        output.writeString(token)
        output.writeString(filterNotRegex)
    }

    fun serializeForShare(output: BinaryOutput) {
        // 2 carried ageIdentity, dropped with the age feature; still consumed
        // when reading version 2 shares.
        output.writeInt(3)

        output.writeInt(type)
        output.writeString(link)

        output.writeBoolean(forceResolve)
        output.writeBoolean(deduplication)
        output.writeBoolean(updateWhenConnectedOnly)
        output.writeString(customUserAgent)
        output.writeString(token)
    }

    override fun deserializeFromBuffer(input: BinaryInput) {
        val version = input.readInt()

        type = input.readInt()
        link = input.readString()
        forceResolve = input.readBoolean()
        deduplication = input.readBoolean()
        updateWhenConnectedOnly = input.readBoolean()
        customUserAgent = input.readString()
        autoUpdate = input.readBoolean()
        autoUpdateDelay = input.readInt()
        lastUpdated = input.readInt()

        if (version >= 2) {
            expiryDate = input.readLong()
            bytesUsed = input.readLong()
            bytesRemaining = input.readLong()
        }

        if (version >= 3) {
            token = input.readString()
        }

        if (version >= 4) {
            filterNotRegex = input.readString()
        }

        if (version == 5) {
            input.readString() // ageIdentity, dropped
        }
    }

    fun deserializeFromShare(input: BinaryInput) {
        val version = input.readInt()

        type = input.readInt()
        link = input.readString()
        forceResolve = input.readBoolean()
        deduplication = input.readBoolean()
        updateWhenConnectedOnly = input.readBoolean()
        customUserAgent = input.readString()

        if (version >= 1) {
            token = input.readString()
        }

        if (version == 2) {
            input.readString() // ageIdentity, dropped
        }
    }

}
