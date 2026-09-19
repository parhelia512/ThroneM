package io.throneproj.thronem.warp

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * ECDSA P-256 keygen + DER serialization for MASQUE-WARP, ported from LxBox's
 * MasqueKeys. The byte contract with the core (sing-box-lx SPEC 021):
 * - `private_key` = base64(SEC1 `ECPrivateKey` DER) — parsed via
 *   `x509.ParseECPrivateKey`.
 * - `public_key`  = base64(PKIX/SPKI DER)          — parsed via
 *   `x509.ParsePKIXPublicKey`.
 *
 * The platform `KeyPairGenerator` produces the keypair; the SEC1/PKIX
 * encodings are assembled by hand from the raw d/x/y coordinates — the JCA
 * default encodings (PKCS#8 / X.509) do not match what Go's x509 parser
 * expects for these fields. base64 is standard (padded), NOT url-safe: the
 * core decodes `base64.StdEncoding`.
 */
object MasqueKeys {

    /** P-256 named curve OID `prime256v1` = `1.2.840.10045.3.1.7`. */
    private const val OID_P256 = "1.2.840.10045.3.1.7"

    /** `id-ecPublicKey` = `1.2.840.10045.2.1`. */
    private const val OID_EC_PUBLIC_KEY = "1.2.840.10045.2.1"

    /** Length of each P-256 coordinate in bytes (256 bit). */
    private const val COORD_LEN = 32

    fun generate(): MasqueKeyMaterial {
        val generator = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        }
        val pair = generator.generateKeyPair()
        val priv = pair.private as ECPrivateKey
        val pub = pair.public as ECPublicKey

        val d = bigIntToFixed(priv.s, COORD_LEN)
        val x = bigIntToFixed(pub.w.affineX, COORD_LEN)
        val y = bigIntToFixed(pub.w.affineY, COORD_LEN)

        val encoder = Base64.getEncoder()
        return MasqueKeyMaterial(
            privateKeyDer = encoder.encodeToString(encodeSec1PrivateKey(d, x, y)),
            publicKeyDer = encoder.encodeToString(encodePkixPublicKey(x, y)),
        )
    }

    /**
     * Strips the PEM armor from a server public key and returns bare
     * base64(DER). Cloudflare sends the pubkey as PEM; the core wants plain
     * base64(DER). Values that are already base64 pass through unchanged.
     */
    fun normalizeServerPubKey(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.contains("-----BEGIN")) {
            return trimmed.replace(Regex("\\s"), "")
        }
        val body = trimmed
            .replace(Regex("-----(BEGIN|END)[^-]*-----"), "")
            .replace(Regex("\\s"), "")
        Base64.getDecoder().decode(body)
        return body
    }

    /** BigInt → big-endian [len]-byte buffer (left zero padded). */
    private fun bigIntToFixed(value: BigInteger, len: Int): ByteArray {
        val bigEndian = value.toByteArray()
        val out = ByteArray(len)
        when {
            bigEndian.size >= len -> {
                // Skip a leading sign byte when the magnitude fits in len.
                val offset = bigEndian.size - len
                if (offset == 1 && bigEndian[0] == 0.toByte()) {
                    bigEndian.copyInto(out, 0, 1)
                } else {
                    bigEndian.copyInto(out, 0, offset)
                }
            }

            else -> bigEndian.copyInto(out, len - bigEndian.size)
        }
        return out
    }

    /** Uncompressed EC point `0x04 ‖ X ‖ Y` (65 bytes for P-256). */
    private fun uncompressedPoint(x: ByteArray, y: ByteArray): ByteArray {
        val out = ByteArray(1 + x.size + y.size)
        out[0] = 0x04
        x.copyInto(out, 1)
        y.copyInto(out, 1 + x.size)
        return out
    }

    /**
     * SEC1 `ECPrivateKey` (RFC 5915), as expected by `x509.ParseECPrivateKey`:
     *
     * ```
     * ECPrivateKey ::= SEQUENCE {
     *   version        INTEGER { ecPrivkeyVer1(1) },
     *   privateKey     OCTET STRING,          -- d, 32 bytes
     *   parameters [0] EXPLICIT ECParameters, -- namedCurve OID prime256v1
     *   publicKey  [1] EXPLICIT BIT STRING    -- 0x04‖X‖Y
     * }
     * ```
     */
    private fun encodeSec1PrivateKey(d: ByteArray, x: ByteArray, y: ByteArray): ByteArray {
        val seq = derSequence(
            derInteger(BigInteger.ONE),
            derTag(0x04, d),
            derContextExplicit(0xA0, derOid(OID_P256)),
            derContextExplicit(0xA1, derBitString(uncompressedPoint(x, y))),
        )
        return seq
    }

    /**
     * PKIX/SPKI `SubjectPublicKeyInfo`, as expected by
     * `x509.ParsePKIXPublicKey`:
     *
     * ```
     * SubjectPublicKeyInfo ::= SEQUENCE {
     *   algorithm SEQUENCE {
     *     algorithm  OID id-ecPublicKey,
     *     parameters OID prime256v1
     *   },
     *   subjectPublicKey BIT STRING  -- 0x04‖X‖Y
     * }
     * ```
     */
    private fun encodePkixPublicKey(x: ByteArray, y: ByteArray): ByteArray {
        val algId = derSequence(
            derOid(OID_EC_PUBLIC_KEY),
            derOid(OID_P256),
        )
        return derSequence(algId, derBitString(uncompressedPoint(x, y)))
    }

    // --- Minimal DER encoding (deterministic, no external dependencies) ---

    private fun derLength(len: Int): ByteArray = when {
        len < 0x80 -> byteArrayOf(len.toByte())
        len < 0x100 -> byteArrayOf(0x81.toByte(), len.toByte())
        else -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xFF).toByte())
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray =
        byteArrayOf(tag.toByte()) + derLength(content.size) + content

    private fun derSequence(vararg parts: ByteArray): ByteArray {
        val content = parts.reduce { acc, bytes -> acc + bytes }
        return derTag(0x30, content)
    }

    private fun derInteger(value: BigInteger): ByteArray =
        derTag(0x02, value.toByteArray())

    private fun derBitString(content: ByteArray): ByteArray =
        derTag(0x03, byteArrayOf(0x00) + content)

    /** DER object identifier from dotted string (X.690 8.19). */
    private fun derOid(dotted: String): ByteArray {
        val parts = dotted.split('.').map { it.toLong() }
        val body = mutableListOf<Byte>()
        body.add((parts[0] * 40 + parts[1]).toByte())
        for (part in parts.drop(2)) {
            var value = part
            val encoded = mutableListOf<Byte>()
            encoded.add((value and 0x7F).toByte())
            value = value ushr 7
            while (value > 0) {
                encoded.add(0, ((value and 0x7F) or 0x80L).toByte())
                value = value ushr 7
            }
            body.addAll(encoded)
        }
        return derTag(0x06, body.toByteArray())
    }

    /** EXPLICIT context-specific tag `[n]` around an already encoded DER object. */
    private fun derContextExplicit(tag: Int, innerDer: ByteArray): ByteArray =
        derTag(tag, innerDer)
}

/** Generated ECDSA P-256 keypair, ready to send to Cloudflare and the core. */
data class MasqueKeyMaterial(
    /** base64(SEC1 DER) private key — SECRET, never log. */
    val privateKeyDer: String,
    /** base64(PKIX DER) client public key (sent to CF in the enroll PATCH). */
    val publicKeyDer: String,
)
