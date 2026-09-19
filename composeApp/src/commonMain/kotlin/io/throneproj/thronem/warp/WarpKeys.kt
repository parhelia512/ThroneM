package io.throneproj.thronem.warp

import java.math.BigInteger
import java.security.SecureRandom

/**
 * X25519 (RFC 7748) scalar multiplication on Curve25519, implemented with
 * BigInteger's Montgomery ladder. Used to generate WireGuard keypairs for
 * WARP registration entirely on-device — the private key never leaves it.
 *
 * Verified against the RFC 7748 section 5.2 test vectors and the section 6.1
 * Diffie-Hellman vector.
 */
object X25519 {

    private val P: BigInteger = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19))
    private const val A24 = 121665L
    private val BASE_POINT_LE = ByteArray(32).also { it[0] = 9 }
    private val U_MASK = BigInteger.TWO.pow(255).subtract(BigInteger.ONE)

    /** Generates 32 random bytes for use as a WireGuard private key. */
    fun generatePrivateKey(): ByteArray =
        ByteArray(32).also { SecureRandom().nextBytes(it) }

    /** Derives the standard-base64-able WireGuard public key for [privateKey]. */
    fun publicKey(privateKey: ByteArray): ByteArray =
        scalarMult(privateKey, BASE_POINT_LE)

    /** Computes the shared/derived point for [scalarBytes] and u-coordinate [uBytes]. */
    fun scalarMult(scalarBytes: ByteArray, uBytes: ByteArray): ByteArray =
        bigIntToLittleEndian(ladder(decodeScalar(scalarBytes), decodeU(uBytes)))

    private fun decodeScalar(bytes: ByteArray): BigInteger {
        require(bytes.size == 32) { "x25519 scalar must be 32 bytes" }
        val clamped = bytes.copyOf()
        clamped[0] = (clamped[0].toInt() and 248).toByte()
        clamped[31] = ((clamped[31].toInt() and 127) or 64).toByte()
        return littleEndianToBigInt(clamped)
    }

    private fun decodeU(bytes: ByteArray): BigInteger {
        require(bytes.size == 32) { "x25519 u-coordinate must be 32 bytes" }
        val masked = bytes.copyOf()
        masked[31] = (masked[31].toInt() and 127).toByte()
        return littleEndianToBigInt(masked).mod(U_MASK)
    }

    private fun ladder(k: BigInteger, u: BigInteger): BigInteger {
        val x1 = u
        var x2 = BigInteger.ONE
        var z2 = BigInteger.ZERO
        var x3 = x1
        var z3 = BigInteger.ONE
        var swap = false
        val a24 = BigInteger.valueOf(A24)
        for (t in 254 downTo 0) {
            val kt = k.testBit(t)
            if (swap != kt) {
                var tmp = x2; x2 = x3; x3 = tmp
                tmp = z2; z2 = z3; z3 = tmp
            }
            swap = kt
            val a = x2.add(z2).mod(P)
            val aa = a.multiply(a).mod(P)
            val b = x2.subtract(z2).mod(P)
            val bb = b.multiply(b).mod(P)
            val e = aa.subtract(bb).mod(P)
            val c = x3.add(z3).mod(P)
            val d = x3.subtract(z3).mod(P)
            val da = d.multiply(a).mod(P)
            val cb = c.multiply(b).mod(P)
            val t0 = da.add(cb)
            x3 = t0.multiply(t0).mod(P)
            val t1 = da.subtract(cb)
            z3 = x1.multiply(t1).multiply(t1).mod(P)
            x2 = aa.multiply(bb).mod(P)
            val t2 = aa.add(e.multiply(a24).mod(P))
            z2 = e.multiply(t2).mod(P)
        }
        if (swap) {
            var tmp = x2; x2 = x3; x3 = tmp
            tmp = z2; z2 = z3; z3 = tmp
        }
        return x2.multiply(z2.modPow(P.subtract(BigInteger.TWO), P)).mod(P)
    }

    private fun littleEndianToBigInt(bytes: ByteArray): BigInteger {
        val bigEndian = ByteArray(bytes.size)
        for (i in bytes.indices) {
            bigEndian[i] = bytes[bytes.size - 1 - i]
        }
        return BigInteger(1, bigEndian)
    }

    private fun bigIntToLittleEndian(value: BigInteger): ByteArray {
        val bigEndian = value.toByteArray()
        var start = 0
        if (bigEndian.size > 32 && bigEndian[0] == 0.toByte()) start = 1
        val out = ByteArray(32)
        var i = 0
        var index = bigEndian.size - 1
        while (index >= start && i < 32) {
            out[i] = bigEndian[index]
            i++
            index--
        }
        return out
    }
}

/** WireGuard key material in the standard base64 encoding. */
data class WireGuardKeyPair(val privateKey: String, val publicKey: String)

fun generateWireGuardKeyPair(): WireGuardKeyPair {
    val priv = X25519.generatePrivateKey()
    val pub = X25519.publicKey(priv)
    return WireGuardKeyPair(
        privateKey = priv.b64Standard(),
        publicKey = pub.b64Standard(),
    )
}

private fun ByteArray.b64Standard(): String =
    java.util.Base64.getEncoder().encodeToString(this)
