@file:Suppress("UNCHECKED_CAST")

package io.throneproj.thronem.fmt.v2ray

import io.throneproj.thronem.fmt.SingBoxOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayTransportOptions_V2RayXHTTPOptions
import io.throneproj.thronem.fmt.SingBoxOptions.V2RayXHTTPXmuxOptions
import io.throneproj.thronem.fmt.buildHeader
import io.throneproj.thronem.fmt.parseHeader
import io.throneproj.thronem.ktx.JSONMap
import io.throneproj.thronem.ktx.URL
import io.throneproj.thronem.ktx.blankAsNull
import io.throneproj.thronem.ktx.getStr
import io.throneproj.thronem.ktx.kxs
import io.throneproj.thronem.ktx.listByLineOrComma
import io.throneproj.thronem.ktx.queryParameterNotBlank
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * XHTTP transport (lx fork only, `with_xhttp`) — URI/JSON mapping for
 * [StandardV2RayBean]. Ported from LxBox's `XhttpTransport`
 * (`lib/models/transport_spec.dart`, `lib/services/parser/transport.dart`).
 *
 * Host / path / headers live on the shared transport fields, exactly like
 * `httpupgrade`; the remaining parameters mirror the core's
 * `option.V2RayXHTTPOptions` (docs-lx/lx-protocols-transports.md §1).
 *
 * Two contracts matter:
 * - Share links use the Xray-style camelCase names on write (`xPaddingBytes`),
 *   and accept both camelCase and sing-box snake_case on read.
 * - The core rejects the WHOLE config on an invalid enum or range, so emit
 *   filters every enum/range parameter instead of passing garbage through
 *   (the same reason LxBox gates `seq_placement`/`x_padding_*`).
 */
private val XHTTP_MODES = setOf("auto", "packet-up", "stream-up", "stream-one")
private val XHTTP_SESSION_PLACEMENTS = setOf("path", "query", "header", "cookie")
private val XHTTP_UPLINK_PLACEMENTS = setOf("body", "auto", "header", "cookie")
private val XHTTP_PADDING_PLACEMENTS = setOf("cookie", "header", "query", "queryInHeader")
private val XHTTP_PADDING_METHODS = setOf("repeat-x", "tokenish")

/** Keys carried flat on the link; never merged in from the `extra` object. */
private val XHTTP_FLAT_ONLY_KEYS = setOf("host", "path", "mode")

/** `"N"` / `"N-M"` (an Xray `[N,M]` array is normalized to `"N-M"`). */
internal fun normalizeXhttpRange(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (value.startsWith("[") && value.endsWith("]")) {
        val parts = value.substring(1, value.length - 1).split(',').map { it.trim() }
        if (parts.size == 2 && parts.all { it.toLongOrNull() != null }) {
            return "${parts[0]}-${parts[1]}"
        }
        return null
    }
    val parts = value.split('-').map { it.trim() }
    if (parts.size > 2 || parts.isEmpty()) return null
    if (parts.any { it.toLongOrNull() == null }) return null
    return parts.joinToString("-")
}

private fun enumOrNull(value: String, allowed: Set<String>): String? =
    value.trim().takeIf { it in allowed }

private fun positiveOrNull(value: Int): Int? = value.takeIf { it >= 0 }

// ───────────────────────────── share link ─────────────────────────────

/**
 * Xray writes camelCase and sing-box writes snake_case; read both (camelCase
 * wins, like LxBox's `_pick`).
 */
private fun URL.xhttpParameter(camelCase: String, snakeCase: String = camelCase): String =
    queryParameter(camelCase).ifBlank { queryParameter(snakeCase) }

/** Strips the `?ed=<n>` early-data suffix Xray appends to the path. */
internal fun stripXhttpEarlyData(path: String): String {
    val index = path.indexOf("?ed=")
    return if (index >= 0) path.substring(0, index) else path
}

/** Reads every xhttp query parameter (flat + `extra`) into [bean]. */
internal fun URL.intoXhttp(bean: StandardV2RayBean) {
    bean.host = xhttpParameter("host")
    bean.path = stripXhttpEarlyData(xhttpParameter("path"))
    bean.xhttpMode = xhttpParameter("mode").ifBlank { "auto" }
    bean.xhttpPaddingBytes = xhttpParameter("xPaddingBytes", "x_padding_bytes")
    bean.xhttpNoGrpcHeader = xhttpParameter("noGRPCHeader", "no_grpc_header").isTruthy()
    bean.xhttpSessionPlacement = xhttpParameter("sessionPlacement", "session_placement")
    bean.xhttpSessionKey = xhttpParameter("sessionKey", "session_key")
    bean.xhttpSeqPlacement = xhttpParameter("seqPlacement", "seq_placement")
    bean.xhttpSeqKey = xhttpParameter("seqKey", "seq_key")
    bean.xhttpSessionTable = xhttpParameter("sessionTable", "session_table")
    bean.xhttpSessionLength = xhttpParameter("sessionLength", "session_length")
    bean.xhttpUplinkDataPlacement = xhttpParameter("uplinkDataPlacement", "uplink_data_placement")
    bean.xhttpUplinkDataKey = xhttpParameter("uplinkDataKey", "uplink_data_key")
    bean.xhttpUplinkChunkSize = xhttpParameter("uplinkChunkSize", "uplink_chunk_size")
    bean.xhttpUplinkHttpMethod = xhttpParameter("uplinkHTTPMethod", "uplink_http_method")
    bean.xhttpPaddingObfsMode = xhttpParameter("xPaddingObfsMode", "x_padding_obfs_mode").isTruthy()
    bean.xhttpPaddingKey = xhttpParameter("xPaddingKey", "x_padding_key")
    bean.xhttpPaddingHeader = xhttpParameter("xPaddingHeader", "x_padding_header")
    bean.xhttpPaddingPlacement = xhttpParameter("xPaddingPlacement", "x_padding_placement")
    bean.xhttpPaddingMethod = xhttpParameter("xPaddingMethod", "x_padding_method")
    bean.xhttpScMaxEachPostBytes = xhttpParameter("scMaxEachPostBytes", "sc_max_each_post_bytes")
    bean.xhttpScMinPostsIntervalMs =
        xhttpParameter("scMinPostsIntervalMs", "sc_min_posts_interval_ms")
    bean.xhttpScStreamUpServerSecs =
        xhttpParameter("scStreamUpServerSecs", "sc_stream_up_server_secs")
    bean.xhttpScMaxBufferedPosts =
        xhttpParameter("scMaxBufferedPosts", "sc_max_buffered_posts").toIntOrNull() ?: -1
    bean.xhttpNoSseHeader = xhttpParameter("noSSEHeader", "no_sse_header").isTruthy()
    bean.xhttpMaxConcurrency = xhttpParameter("maxConcurrency", "max_concurrency")
    bean.xhttpMaxConnections = xhttpParameter("maxConnections", "max_connections")
    bean.xhttpCMaxReuseTimes = xhttpParameter("cMaxReuseTimes", "c_max_reuse_times")
    bean.xhttpHMaxRequestTimes = xhttpParameter("hMaxRequestTimes", "h_max_request_times")
    bean.xhttpHMaxReusableSecs = xhttpParameter("hMaxReusableSecs", "h_max_reusable_secs")
    bean.xhttpHKeepAlivePeriod =
        xhttpParameter("hKeepAlivePeriod", "h_keep_alive_period").toIntOrNull() ?: -1

    queryParameterNotBlank("extra")?.let { mergeXhttpExtra(bean, it) }
}

/**
 * Xray's `extra` carries the same parameters as a URL-encoded JSON object;
 * `host`/`path`/`mode` are always flat, and a nested `xmux` object is
 * flattened. Only unset bean fields are filled (the flat link params win).
 */
private fun mergeXhttpExtra(bean: StandardV2RayBean, raw: String) {
    val root = try {
        kxs.parseToJsonElement(raw) as? JsonObject
    } catch (_: Exception) {
        null
    } ?: return

    val extra = LinkedHashMap<String, String>()
    for ((key, value) in root) {
        if (key in XHTTP_FLAT_ONLY_KEYS || key == "xmux") continue
        (value as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { extra[key] = it }
    }
    (root["xmux"] as? JsonObject)?.forEach { (key, value) ->
        (value as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { extra[key] = it }
    }

    fun fill(camelCase: String, snakeCase: String, current: String, apply: (String) -> Unit) {
        if (current.isNotBlank()) return
        val value = extra[camelCase] ?: extra[snakeCase] ?: return
        apply(value)
    }

    fill("xPaddingBytes", "x_padding_bytes", bean.xhttpPaddingBytes) { bean.xhttpPaddingBytes = it }
    fill("sessionPlacement", "session_placement", bean.xhttpSessionPlacement) {
        bean.xhttpSessionPlacement = it
    }
    fill("sessionKey", "session_key", bean.xhttpSessionKey) { bean.xhttpSessionKey = it }
    fill("seqPlacement", "seq_placement", bean.xhttpSeqPlacement) { bean.xhttpSeqPlacement = it }
    fill("seqKey", "seq_key", bean.xhttpSeqKey) { bean.xhttpSeqKey = it }
    fill("sessionTable", "session_table", bean.xhttpSessionTable) { bean.xhttpSessionTable = it }
    fill("sessionLength", "session_length", bean.xhttpSessionLength) { bean.xhttpSessionLength = it }
    fill("uplinkDataPlacement", "uplink_data_placement", bean.xhttpUplinkDataPlacement) {
        bean.xhttpUplinkDataPlacement = it
    }
    fill("uplinkDataKey", "uplink_data_key", bean.xhttpUplinkDataKey) { bean.xhttpUplinkDataKey = it }
    fill("uplinkChunkSize", "uplink_chunk_size", bean.xhttpUplinkChunkSize) {
        bean.xhttpUplinkChunkSize = it
    }
    fill("uplinkHTTPMethod", "uplink_http_method", bean.xhttpUplinkHttpMethod) {
        bean.xhttpUplinkHttpMethod = it
    }
    fill("xPaddingKey", "x_padding_key", bean.xhttpPaddingKey) { bean.xhttpPaddingKey = it }
    fill("xPaddingHeader", "x_padding_header", bean.xhttpPaddingHeader) {
        bean.xhttpPaddingHeader = it
    }
    fill("xPaddingPlacement", "x_padding_placement", bean.xhttpPaddingPlacement) {
        bean.xhttpPaddingPlacement = it
    }
    fill("xPaddingMethod", "x_padding_method", bean.xhttpPaddingMethod) {
        bean.xhttpPaddingMethod = it
    }
    fill("scMaxEachPostBytes", "sc_max_each_post_bytes", bean.xhttpScMaxEachPostBytes) {
        bean.xhttpScMaxEachPostBytes = it
    }
    fill("scMinPostsIntervalMs", "sc_min_posts_interval_ms", bean.xhttpScMinPostsIntervalMs) {
        bean.xhttpScMinPostsIntervalMs = it
    }
    fill("scStreamUpServerSecs", "sc_stream_up_server_secs", bean.xhttpScStreamUpServerSecs) {
        bean.xhttpScStreamUpServerSecs = it
    }
    fill("noSSEHeader", "no_sse_header", if (bean.xhttpNoSseHeader) "1" else "") {
        bean.xhttpNoSseHeader = it.isTruthy()
    }
    fill("maxConcurrency", "max_concurrency", bean.xhttpMaxConcurrency) {
        bean.xhttpMaxConcurrency = it
    }
    fill("maxConnections", "max_connections", bean.xhttpMaxConnections) {
        bean.xhttpMaxConnections = it
    }
    fill("cMaxReuseTimes", "c_max_reuse_times", bean.xhttpCMaxReuseTimes) {
        bean.xhttpCMaxReuseTimes = it
    }
    fill("hMaxRequestTimes", "h_max_request_times", bean.xhttpHMaxRequestTimes) {
        bean.xhttpHMaxRequestTimes = it
    }
    fill("hMaxReusableSecs", "h_max_reusable_secs", bean.xhttpHMaxReusableSecs) {
        bean.xhttpHMaxReusableSecs = it
    }
    fill("hKeepAlivePeriod", "h_keep_alive_period", bean.xhttpHKeepAlivePeriod.takeIf { it >= 0 }
        ?.toString().orEmpty()) {
        bean.xhttpHKeepAlivePeriod = it.toIntOrNull() ?: -1
    }
}

private fun String.isTruthy(): Boolean =
    lowercase().let { it == "true" || it == "1" || it == "on" }

/** Writes the non-default xhttp parameters in Xray's camelCase form. */
internal fun StandardV2RayBean.writeXhttpQuery(builder: URL) {
    builder.addQueryParameter("mode", xhttpMode.ifBlank { "auto" })
    if (xhttpPaddingBytes.isNotBlank()) {
        builder.addQueryParameter("xPaddingBytes", xhttpPaddingBytes)
    }
    if (xhttpNoGrpcHeader) builder.addQueryParameter("noGRPCHeader", "true")
    xhttpSessionPlacement.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("sessionPlacement", it) }
    xhttpSessionKey.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("sessionKey", it) }
    xhttpSeqPlacement.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("seqPlacement", it) }
    xhttpSeqKey.takeIf { it.isNotBlank() }?.let { builder.addQueryParameter("seqKey", it) }
    xhttpSessionTable.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("sessionTable", it) }
    xhttpSessionLength.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("sessionLength", it) }
    xhttpUplinkDataPlacement.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("uplinkDataPlacement", it) }
    xhttpUplinkDataKey.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("uplinkDataKey", it) }
    xhttpUplinkChunkSize.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("uplinkChunkSize", it) }
    xhttpUplinkHttpMethod.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("uplinkHTTPMethod", it) }
    if (xhttpPaddingObfsMode) builder.addQueryParameter("xPaddingObfsMode", "true")
    xhttpPaddingKey.takeIf { it.isNotBlank() }?.let { builder.addQueryParameter("xPaddingKey", it) }
    xhttpPaddingHeader.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("xPaddingHeader", it) }
    xhttpPaddingPlacement.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("xPaddingPlacement", it) }
    xhttpPaddingMethod.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("xPaddingMethod", it) }
    xhttpScMaxEachPostBytes.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("scMaxEachPostBytes", it) }
    xhttpScMinPostsIntervalMs.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("scMinPostsIntervalMs", it) }
    xhttpScStreamUpServerSecs.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("scStreamUpServerSecs", it) }
    positiveOrNull(xhttpScMaxBufferedPosts)?.let {
        builder.addQueryParameter("scMaxBufferedPosts", "$it")
    }
    if (xhttpNoSseHeader) builder.addQueryParameter("noSSEHeader", "true")
    xhttpMaxConcurrency.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("maxConcurrency", it) }
    xhttpMaxConnections.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("maxConnections", it) }
    xhttpCMaxReuseTimes.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("cMaxReuseTimes", it) }
    xhttpHMaxRequestTimes.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("hMaxRequestTimes", it) }
    xhttpHMaxReusableSecs.takeIf { it.isNotBlank() }
        ?.let { builder.addQueryParameter("hMaxReusableSecs", it) }
    positiveOrNull(xhttpHKeepAlivePeriod)?.let {
        builder.addQueryParameter("hKeepAlivePeriod", "$it")
    }
}

// ───────────────────────── sing-box config ─────────────────────────

/** Builds the core's `transport` object for the xhttp outbound. */
internal fun buildSingBoxXhttpTransport(
    bean: StandardV2RayBean,
): V2RayTransportOptions_V2RayXHTTPOptions =
    V2RayTransportOptions_V2RayXHTTPOptions().apply {
        type = SingBoxOptions.TRANSPORT_XHTTP
        host = bean.host.listByLineOrComma().firstOrNull()
        path = bean.path.blankAsNull()
        headers = bean.headers.blankAsNull()?.let(::buildHeader)?.toMutableMap()

        no_grpc_header = bean.xhttpNoGrpcHeader.takeIf { it }?.let { true }
        x_padding_obfs_mode = bean.xhttpPaddingObfsMode.takeIf { it }?.let { true }

        x_padding_bytes = normalizeXhttpRange(bean.xhttpPaddingBytes)
        session_placement = enumOrNull(bean.xhttpSessionPlacement, XHTTP_SESSION_PLACEMENTS)
        session_key = bean.xhttpSessionKey.blankAsNull()
        session_table = bean.xhttpSessionTable.blankAsNull()
        session_length = normalizeXhttpRange(bean.xhttpSessionLength)

        // A header/cookie uplink placement is only legal in packet-up mode:
        // force packet-up when the mode is auto, drop the placement otherwise.
        val uplinkPlacement = enumOrNull(bean.xhttpUplinkDataPlacement, XHTTP_UPLINK_PLACEMENTS)
        val requestedMode = enumOrNull(bean.xhttpMode, XHTTP_MODES) ?: "auto"
        val uplinkNeedsPacketUp = uplinkPlacement == "header" || uplinkPlacement == "cookie"
        mode = when {
            uplinkNeedsPacketUp && requestedMode == "auto" -> "packet-up"
            else -> requestedMode
        }
        uplink_data_placement = uplinkPlacement?.takeIf {
            !uplinkNeedsPacketUp || mode == "packet-up"
        }
        uplink_data_key = bean.xhttpUplinkDataKey.blankAsNull()
        uplink_chunk_size = normalizeXhttpRange(bean.xhttpUplinkChunkSize)
        uplink_http_method = bean.xhttpUplinkHttpMethod.blankAsNull()?.uppercase()

        seq_placement = enumOrNull(bean.xhttpSeqPlacement, XHTTP_SESSION_PLACEMENTS)
        seq_key = bean.xhttpSeqKey.blankAsNull()

        x_padding_key = bean.xhttpPaddingKey.blankAsNull()
        x_padding_header = bean.xhttpPaddingHeader.blankAsNull()
        x_padding_placement = enumOrNull(bean.xhttpPaddingPlacement, XHTTP_PADDING_PLACEMENTS)
        x_padding_method = enumOrNull(bean.xhttpPaddingMethod, XHTTP_PADDING_METHODS)

        sc_max_each_post_bytes = normalizeXhttpRange(bean.xhttpScMaxEachPostBytes)
        sc_min_posts_interval_ms = normalizeXhttpRange(bean.xhttpScMinPostsIntervalMs)
        sc_stream_up_server_secs = normalizeXhttpRange(bean.xhttpScStreamUpServerSecs)
        sc_max_buffered_posts = positiveOrNull(bean.xhttpScMaxBufferedPosts)?.toLong()
        no_sse_header = bean.xhttpNoSseHeader.takeIf { it }?.let { true }

        val xmuxOptions = V2RayXHTTPXmuxOptions().apply {
            max_concurrency = normalizeXhttpRange(bean.xhttpMaxConcurrency)
            max_connections = normalizeXhttpRange(bean.xhttpMaxConnections)
            c_max_reuse_times = normalizeXhttpRange(bean.xhttpCMaxReuseTimes)
            h_max_request_times = normalizeXhttpRange(bean.xhttpHMaxRequestTimes)
            h_max_reusable_secs = normalizeXhttpRange(bean.xhttpHMaxReusableSecs)
            h_keep_alive_period = positiveOrNull(bean.xhttpHKeepAlivePeriod)?.toLong()
        }
        xmux = xmuxOptions.takeIf {
            it.max_concurrency != null || it.max_connections != null ||
                it.c_max_reuse_times != null || it.h_max_request_times != null ||
                it.h_max_reusable_secs != null || it.h_keep_alive_period != null
        }
    }

/** Fills the shared transport fields + xhttp parameters from parsed JSON. */
internal fun StandardV2RayBean.applyXhttpJson(transport: V2RayTransportOptions_V2RayXHTTPOptions) {
    host = transport.host.orEmpty()
    path = transport.path.orEmpty()
    headers = transport.headers?.let {
        parseHeader(it).map { entry ->
            entry.key + ":" + entry.value.joinToString(",")
        }.joinToString("\n")
    }.orEmpty()

    xhttpMode = transport.mode.orEmpty().ifBlank { "auto" }
    xhttpPaddingBytes = transport.x_padding_bytes.orEmpty()
    xhttpNoGrpcHeader = transport.no_grpc_header == true
    xhttpSessionPlacement = transport.session_placement.orEmpty()
    xhttpSessionKey = transport.session_key.orEmpty()
    xhttpSeqPlacement = transport.seq_placement.orEmpty()
    xhttpSeqKey = transport.seq_key.orEmpty()
    xhttpSessionTable = transport.session_table.orEmpty()
    xhttpSessionLength = transport.session_length.orEmpty()
    xhttpUplinkDataPlacement = transport.uplink_data_placement.orEmpty()
    xhttpUplinkDataKey = transport.uplink_data_key.orEmpty()
    xhttpUplinkChunkSize = transport.uplink_chunk_size.orEmpty()
    xhttpUplinkHttpMethod = transport.uplink_http_method.orEmpty()
    xhttpPaddingObfsMode = transport.x_padding_obfs_mode == true
    xhttpPaddingKey = transport.x_padding_key.orEmpty()
    xhttpPaddingHeader = transport.x_padding_header.orEmpty()
    xhttpPaddingPlacement = transport.x_padding_placement.orEmpty()
    xhttpPaddingMethod = transport.x_padding_method.orEmpty()
    xhttpScMaxEachPostBytes = transport.sc_max_each_post_bytes.orEmpty()
    xhttpScMinPostsIntervalMs = transport.sc_min_posts_interval_ms.orEmpty()
    xhttpScStreamUpServerSecs = transport.sc_stream_up_server_secs.orEmpty()
    xhttpScMaxBufferedPosts = transport.sc_max_buffered_posts?.toInt() ?: -1
    xhttpNoSseHeader = transport.no_sse_header == true

    transport.xmux?.let { xmux ->
        xhttpMaxConcurrency = xmux.max_concurrency.orEmpty()
        xhttpMaxConnections = xmux.max_connections.orEmpty()
        xhttpCMaxReuseTimes = xmux.c_max_reuse_times.orEmpty()
        xhttpHMaxRequestTimes = xmux.h_max_request_times.orEmpty()
        xhttpHMaxReusableSecs = xmux.h_max_reusable_secs.orEmpty()
        xhttpHKeepAlivePeriod = xmux.h_keep_alive_period?.toInt() ?: -1
    }
}

/** Reads a raw `transport` JSON object (import path, incl. nested `xmux`). */
internal fun parseXhttpJson(json: JSONMap): V2RayTransportOptions_V2RayXHTTPOptions =
    V2RayTransportOptions_V2RayXHTTPOptions().apply {
        type = SingBoxOptions.TRANSPORT_XHTTP
        host = json.getStr("host")
        path = json.getStr("path")
        mode = json.getStr("mode")
        headers = (json["headers"] as? JSONMap)?.let { parseHeader(it).toMutableMap() }
        x_padding_bytes = json.getStr("x_padding_bytes")
        no_grpc_header = (json["no_grpc_header"] as? Boolean)
        session_placement = json.getStr("session_placement")
        session_key = json.getStr("session_key")
        seq_placement = json.getStr("seq_placement")
        seq_key = json.getStr("seq_key")
        session_table = json.getStr("session_table")
        session_length = json.getStr("session_length")
        uplink_data_placement = json.getStr("uplink_data_placement")
        uplink_data_key = json.getStr("uplink_data_key")
        uplink_chunk_size = json.getStr("uplink_chunk_size")
        uplink_http_method = json.getStr("uplink_http_method")
        x_padding_obfs_mode = (json["x_padding_obfs_mode"] as? Boolean)
        x_padding_key = json.getStr("x_padding_key")
        x_padding_header = json.getStr("x_padding_header")
        x_padding_placement = json.getStr("x_padding_placement")
        x_padding_method = json.getStr("x_padding_method")
        sc_max_each_post_bytes = json.getStr("sc_max_each_post_bytes")
        sc_min_posts_interval_ms = json.getStr("sc_min_posts_interval_ms")
        sc_stream_up_server_secs = json.getStr("sc_stream_up_server_secs")
        sc_max_buffered_posts = (json["sc_max_buffered_posts"] as? Number)?.toLong()
        no_sse_header = (json["no_sse_header"] as? Boolean)
        (json["xmux"] as? JSONMap)?.let { xmuxJson ->
            xmux = V2RayXHTTPXmuxOptions().apply {
                max_concurrency = xmuxJson.getStr("max_concurrency")
                max_connections = xmuxJson.getStr("max_connections")
                c_max_reuse_times = xmuxJson.getStr("c_max_reuse_times")
                h_max_request_times = xmuxJson.getStr("h_max_request_times")
                h_max_reusable_secs = xmuxJson.getStr("h_max_reusable_secs")
                h_keep_alive_period = (xmuxJson["h_keep_alive_period"] as? Number)?.toLong()
            }
        }
    }
