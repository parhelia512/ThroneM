package io.throneproj.thronem.ktx

/**
 * Kotlin replacement for libcore's `Libcore.parseDuration`, which wrapped
 * sing-box's duration parser. Accepts "0" or one or more `<number><unit>`
 * segments with units ns/us/µs/μs/ms/s/m/h (decimals allowed per segment),
 * mirroring sing's duration semantics. Returns the total in nanoseconds.
 *
 * @throws IllegalArgumentException on anything else.
 */
fun parseDuration(text: String): Long {
    val input = text.trim()
    if (input.isEmpty()) throw IllegalArgumentException("missing unit in duration \"$text\"")
    if (input == "0") return 0L

    var index = 0
    var total = 0.0
    val end = input.length
    while (index < end) {
        val numberStart = index
        while (index < end && (input[index].isDigit() || input[index] == '.')) index++
        if (numberStart == index) {
            throw IllegalArgumentException("invalid duration \"$text\"")
        }
        val value = input.substring(numberStart, index).toDoubleOrNull()
            ?: throw IllegalArgumentException("invalid duration \"$text\"")

        val unitStart = index
        while (index < end && !input[index].isDigit() && input[index] != '.') index++
        when (val unit = input.substring(unitStart, index)) {
            "ns" -> total += value * 1.0
            "us", "µs", "μs" -> total += value * 1_000.0
            "ms" -> total += value * 1_000_000.0
            "s" -> total += value * 1_000_000_000.0
            "m" -> total += value * 60 * 1_000_000_000.0
            "h" -> total += value * 3600 * 1_000_000_000.0
            else -> throw IllegalArgumentException(
                "unknown unit \"$unit\" in duration \"$text\"",
            )
        }
    }
    if (total.isNaN() || total.isInfinite() || total > Long.MAX_VALUE) {
        throw IllegalArgumentException("invalid duration \"$text\"")
    }
    return total.toLong()
}
