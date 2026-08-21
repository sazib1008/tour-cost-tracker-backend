package com.example.tripzy_backend.auth.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret:tripzy-default-super-secret-key-that-must-be-at-least-256-bits-long-for-hmac-sha256}")
    private val jwtSecret: String,

    @Value("\${app.jwt.expiration-ms:2592000000}") // 30 days in ms
    val expirationMs: Long
) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateToken(userId: String, email: String, name: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("name", name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey)
            .compact()
    }

    fun validateAndExtractClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun extractUserId(token: String): String? =
        validateAndExtractClaims(token)?.subject

    fun extractEmail(token: String): String? =
        validateAndExtractClaims(token)?.get("email", String::class.java)

    fun extractName(token: String): String? =
        validateAndExtractClaims(token)?.get("name", String::class.java)
}
