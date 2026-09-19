package io.throneproj.thronem.ktx

import io.nekohasekai.libbox.ConnectionEvent
import io.nekohasekai.libbox.ConnectionEventIterator
import io.nekohasekai.libbox.ConnectionEvents
import io.nekohasekai.libbox.OpenConnectEndpointStatus
import io.nekohasekai.libbox.OpenConnectEndpointStatusIterator
import io.nekohasekai.libbox.OpenVPNEndpointStatus
import io.nekohasekai.libbox.OpenVPNEndpointStatusIterator
import io.nekohasekai.libbox.OutboundGroupItem
import io.nekohasekai.libbox.OutboundGroupItemIterator
import io.nekohasekai.libbox.StringIterator

fun Iterable<String>.toStringIterator(len: Int = -1): StringIterator {
    val iterator = iterator()
    return object : StringIterator {
        override fun hasNext(): Boolean = iterator.hasNext()

        override fun len(): Int = len

        override fun next(): String = iterator.next()
    }
}

fun StringIterator.toList(): List<String> = ArrayList<String>(len().coerceAtLeast(0)).apply {
    while (hasNext()) {
        add(next())
    }
}

/**
 * The gomobile iterator types share no supertype, so each needs its own
 * Materializer. `len()` may report -1 for streamed batches — never rely on it.
 */
fun OutboundGroupItemIterator.toList(): List<OutboundGroupItem> =
    ArrayList<OutboundGroupItem>().apply {
        while (hasNext()) add(next())
    }

fun ConnectionEventIterator.toList(): List<ConnectionEvent> =
    ArrayList<ConnectionEvent>().apply {
        while (hasNext()) add(next())
    }

fun ConnectionEvents.toList(): List<ConnectionEvent> = iterator().toList()

fun OpenConnectEndpointStatusIterator.toList(): List<OpenConnectEndpointStatus> =
    ArrayList<OpenConnectEndpointStatus>().apply {
        while (hasNext()) add(next())
    }

fun OpenVPNEndpointStatusIterator.toList(): List<OpenVPNEndpointStatus> =
    ArrayList<OpenVPNEndpointStatus>().apply {
        while (hasNext()) add(next())
    }
