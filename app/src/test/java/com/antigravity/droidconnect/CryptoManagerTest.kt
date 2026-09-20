package com.antigravity.droidconnect

import com.antigravity.droidconnect.crypto.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun testEmptyKeyReturnsPlaintext() {
        val message = "Hello from Android"
        val encrypted = CryptoManager.encrypt(message, "")
        assertEquals(message, encrypted)

        val decrypted = CryptoManager.decrypt(encrypted, "")
        assertEquals(message, decrypted)
    }

    @Test
    fun testEncryptAndDecryptWithPassphrase() {
        val message = "Top secret notification: Verification code 123456"
        val passphrase = "my-secure-password-2026"

        val encrypted = CryptoManager.encrypt(message, passphrase)
        assertNotEquals(message, encrypted)
        assertTrue(encrypted.isNotBlank())

        val decrypted = CryptoManager.decrypt(encrypted, passphrase)
        assertEquals(message, decrypted)
    }

    @Test
    fun testUniqueIVPerEncryption() {
        val message = "Repeated notification text"
        val passphrase = "same-passphrase"

        val enc1 = CryptoManager.encrypt(message, passphrase)
        val enc2 = CryptoManager.encrypt(message, passphrase)

        // AES-GCM must produce different ciphertexts due to random IV
        assertNotEquals(enc1, enc2)

        // But both must decrypt to the exact same plaintext
        assertEquals(message, CryptoManager.decrypt(enc1, passphrase))
        assertEquals(message, CryptoManager.decrypt(enc2, passphrase))
    }

    @Test(expected = Exception::class)
    fun testDecryptionFailsWithWrongPassphrase() {
        val message = "Private data"
        val encrypted = CryptoManager.encrypt(message, "correct-key")
        CryptoManager.decrypt(encrypted, "wrong-key")
    }
}
