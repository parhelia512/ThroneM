package io.throneproj.thronem.ktx

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

/**
 * Kotlin replacement for the libcore archive helpers removed with the Go core
 * (`Libcore.tryUnpack`, `Libcore.untargzWithoutDir`, `Libcore.extractAssets`).
 *
 * Supported formats are the ones the app consumes: zip (release asset
 * packages, user imports) and tar.gz (GitHub codeload branch archives). The
 * old tar.zst bundled assets are replaced by gzip-compressed files handled by
 * [extractGzipFileTo].
 */

/**
 * Detects the format from the file header and unpacks into [destinationDir].
 * If every entry lives under a single root directory (GitHub style), that
 * level is dropped, matching the old `TryUnpack` behavior.
 */
fun File.tryUnpackTo(destinationDir: File) {
    when (readMagic()) {
        ZIP_MAGIC -> destinationDir.unpackFrom { unzipInto(it) }
        GZIP_MAGIC -> destinationDir.unpackFrom { unTarGzInto(it) }
        else -> throw IllegalArgumentException("Unsupported archive: $name")
    }
    Logs.d("unpacked $name to $destinationDir")
}

/** tar.gz archive: extracts every entry, always stripping the root directory. */
fun File.unTarGzWithoutDirTo(destinationDir: File) {
    destinationDir.unpackFrom { unTarGzInto(it) }
    Logs.d("unpacked (no dir) $name to $destinationDir")
}

private const val ZIP_MAGIC = "PK"
private const val GZIP_MAGIC = "\u001f\u008b"

private fun File.readMagic(): String = inputStream().use { input ->
    val head = ByteArray(2)
    var read = 0
    while (read < head.size) {
        val n = input.read(head, read, head.size - read)
        if (n < 0) break
        read += n
    }
    String(head, 0, read, Charsets.ISO_8859_1)
}

/**
 * Extracts the archive into a scratch directory inside [destinationDir], then
 * moves the content up one level when the archive wraps everything in a single
 * root directory.
 */
private inline fun File.unpackFrom(extract: (File) -> Unit) {
    mkdirs()
    val scratch = resolve("__unpack_tmp").apply { deleteRecursively(); mkdirs() }
    try {
        extract(scratch)
        val entries = scratch.listFiles().orEmpty()
        val source = if (entries.size == 1 && entries[0].isDirectory) entries[0] else scratch
        for (child in source.listFiles().orEmpty()) {
            val target = resolve(child.name)
            if (target.exists()) target.deleteRecursively()
            if (!child.renameTo(target)) {
                if (child.isDirectory) {
                    child.copyRecursively(target, overwrite = true)
                } else {
                    child.copyTo(target, overwrite = true)
                }
            }
        }
    } finally {
        scratch.deleteRecursively()
    }
}

private fun File.unzipInto(destinationDir: File) {
    ZipFile(this).use { zip ->
        for (entry in zip.entries()) {
            val target = destinationDir.safeResolve(entry.name)
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}

private fun File.unTarGzInto(destinationDir: File) {
    destinationDir.mkdirs()
    GZIPInputStream(inputStream().buffered()).use { gunzipped ->
        TarReader(gunzipped).use { reader ->
            while (true) {
                val entry = reader.nextEntry() ?: break
                val target = destinationDir.safeResolve(entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> reader.copyTo(output) }
                }
            }
        }
    }
}

/** Resolves an archive entry name inside [this], rejecting path traversal. */
private fun File.safeResolve(name: String): File {
    val normalized = name.replace('\\', '/')
    val target = resolve(normalized).canonicalFile
    if (!target.startsWith(canonicalFile)) {
        throw SecurityException("Blocked archive entry outside destination: $name")
    }
    return target
}

/** Unpacks a single gzip-compressed payload (e.g. the bundled geoip.db.gz). */
fun File.extractGzipFileTo(target: File) {
    target.parentFile?.mkdirs()
    GZIPInputStream(inputStream().buffered()).use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
}

private class TarEntry(val name: String, val isDirectory: Boolean)

/**
 * Minimal ustar/GNU tar reader. The reader itself is the entry's data stream:
 * after [nextEntry] returns a file entry, read from it to obtain the content.
 */
private class TarReader(private val input: InputStream) : InputStream(), AutoCloseable {

    private val header = ByteArray(BLOCK)

    private var entrySize = 0L
    private var entryRemaining = 0L
    private var padRemaining = 0L
    private var inEntry = false

    /** Returns the next entry, or null at the end-of-archive marker. */
    fun nextEntry(): TarEntry? {
        var longName: String? = null
        if (inEntry) skipEntryTail()
        while (true) {
            if (input.readFully(header) < BLOCK) return null
            if (header.isZeroBlock()) {
                input.skipFully(BLOCK.toLong())
                return null
            }
            val name = parseString(0, 100)
            val size = parseOctal(124, 12)
            when (header[156].toInt().toChar()) {
                // GNU long name: the next data block holds the real entry name.
                'L' -> {
                    val data = ByteArray(size.toInt())
                    input.readFully(data)
                    input.skipFully(padding(size))
                    longName = data.decodeToString().trimEnd('\u0000')
                }

                '5' -> {
                    input.skipFully(size + padding(size))
                    return TarEntry(normalize(longName ?: name), isDirectory = true)
                }

                '0', '\u0000' -> {
                    entrySize = size
                    entryRemaining = size
                    padRemaining = padding(size)
                    inEntry = true
                    return TarEntry(normalize(longName ?: name), isDirectory = false)
                }

                else -> input.skipFully(size + padding(size))
            }
        }
    }

    private fun skipEntryTail() {
        val skip = entryRemaining + padRemaining
        if (skip > 0) input.skipFully(skip)
        inEntry = false
        entryRemaining = 0
        padRemaining = 0
    }

    override fun read(): Int {
        if (!inEntry || entryRemaining == 0L) return -1
        val b = input.read()
        if (b >= 0) entryRemaining--
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (!inEntry || entryRemaining == 0L) return -1
        val n = input.read(b, off, minOf(len.toLong(), entryRemaining).toInt())
        if (n > 0) entryRemaining -= n
        return n
    }

    private fun parseString(offset: Int, length: Int): String {
        var end = offset
        val limit = offset + length
        while (end < limit && header[end] != 0.toByte()) end++
        return String(header, offset, end - offset, Charsets.UTF_8)
    }

    private fun parseOctal(offset: Int, length: Int): Long {
        var result = 0L
        var started = false
        for (i in offset until offset + length) {
            val c = header[i].toInt() and 0xFF
            if (c == 0 || c == ' '.code) {
                if (started) break
                continue
            }
            if (c in '0'.code..'7'.code) {
                started = true
                result = (result shl 3) or (c - '0'.code).toLong()
            } else {
                break
            }
        }
        return result
    }

    private fun ByteArray.isZeroBlock(): Boolean = all { it == 0.toByte() }

    private fun normalize(name: String): String {
        var n = name.trim().trimEnd('/')
        if (n.startsWith("./")) n = n.substring(2)
        return n
    }

    private fun padding(size: Long): Long {
        val remainder = (size % BLOCK).toInt()
        return if (remainder == 0) 0L else (BLOCK - remainder).toLong()
    }

    override fun close() {
        input.close()
    }

    companion object {
        private const val BLOCK = 512
    }
}

private fun InputStream.readFully(target: ByteArray): Int {
    var read = 0
    while (read < target.size) {
        val n = read(target, read, target.size - read)
        if (n < 0) break
        read += n
    }
    if (read < target.size) throw IOException("unexpected end of tar data")
    return read
}

private fun InputStream.skipFully(count: Long) {
    var remaining = count
    while (remaining > 0) {
        val n = skip(remaining)
        if (n <= 0) {
            if (read() < 0) break
            remaining--
        } else {
            remaining -= n
        }
    }
}
