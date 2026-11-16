package com.periodtracker.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Binary v1: magic(8), version(4), KDF iterations(4), salt(16), nonce(12), ciphertext+tag. */
class BackupCipher {
    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        private const val ITERATIONS = 600_000
        private const val HEADER_SIZE = 44
        private val MAGIC = "CYCLEJNL".toByteArray(Charsets.US_ASCII)
    }
    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 10) { "Use a passphrase of at least 10 characters." }
        require(plain.size <= MAX_BYTES - HEADER_SIZE - 16) { "Backup is too large." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE).put(MAGIC).putInt(1).putInt(ITERATIONS).put(salt).put(nonce).array()
        return header + crypt(Cipher.ENCRYPT_MODE, plain, password, salt, nonce, header)
    }
    fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray {
        require(encrypted.size in (HEADER_SIZE + 16)..MAX_BYTES) { "Invalid backup file." }
        val buffer = ByteBuffer.wrap(encrypted)
        val magic = ByteArray(8).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "This is not a Cycle Journal backup." }
        require(buffer.int == 1) { "This backup version is not supported." }
        require(buffer.int == ITERATIONS) { "Unsupported backup encryption parameters." }
        val salt = ByteArray(16).also(buffer::get)
        val nonce = ByteArray(12).also(buffer::get)
        return crypt(Cipher.DECRYPT_MODE, encrypted.copyOfRange(HEADER_SIZE, encrypted.size), password,
            salt, nonce, encrypted.copyOfRange(0, HEADER_SIZE))
    }
    private fun crypt(mode: Int, input: ByteArray, password: CharArray, salt: ByteArray,
        nonce: ByteArray, header: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(header)
                doFinal(input)
            }
        } finally { key.fill(0) }
    }
}
