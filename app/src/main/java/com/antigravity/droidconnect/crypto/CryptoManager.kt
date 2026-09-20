package com.antigravity.droidconnect.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    /**
     * Derives a 256-bit AES SecretKey from any passphrase using SHA-256.
     */
    private fun deriveKey(passphrase: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: Base64([12-byte IV] + [Ciphertext + 16-byte Auth Tag])
     */
    fun encrypt(plainText: String, secretKeyPassphrase: String): String {
        if (secretKeyPassphrase.isBlank()) return plainText

        val key = deriveKey(secretKeyPassphrase)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Prepend IV to ciphertext
        val byteBuffer = ByteBuffer.allocate(iv.size + cipherText.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherText)

        return Base64.getEncoder().encodeToString(byteBuffer.array())
    }

    /**
     * Decrypts Base64-encoded payload encrypted by [encrypt].
     */
    fun decrypt(encryptedBase64: String, secretKeyPassphrase: String): String {
        if (secretKeyPassphrase.isBlank()) return encryptedBase64

        val key = deriveKey(secretKeyPassphrase)
        val combined = Base64.getDecoder().decode(encryptedBase64)

        if (combined.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Ciphertext too short to contain valid IV")
        }

        val byteBuffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(IV_LENGTH_BYTES)
        byteBuffer.get(iv)

        val cipherText = ByteArray(byteBuffer.remaining())
        byteBuffer.get(cipherText)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }
}
