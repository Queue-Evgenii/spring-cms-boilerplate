package com.example.admin.identity.auth.service

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat

object TokenHasher {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }
}
