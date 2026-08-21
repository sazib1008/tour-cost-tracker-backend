package com.example.tripzy_backend.auth.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private val secret = "test-super-secret-key-that-is-at-least-256-bits-long-1234567890"

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(
            jwtSecret = secret,
            expirationMs = 3600000L // 1 hour
        )
    }

    @Test
    fun `should generate valid token and extract claims correctly`() {
        val userId = "user-123"
        val email = "sazib@example.com"
        val name = "Sazib Hossain"

        val token = jwtService.generateToken(userId, email, name)
        assertNotNull(token)

        val extractedUserId = jwtService.extractUserId(token)
        val extractedEmail = jwtService.extractEmail(token)
        val extractedName = jwtService.extractName(token)

        assertEquals(userId, extractedUserId)
        assertEquals(email, extractedEmail)
        assertEquals(name, extractedName)
    }

    @Test
    fun `should return null when token is malformed`() {
        val claims = jwtService.validateAndExtractClaims("invalid.malformed.token")
        assertNull(claims)
    }

    @Test
    fun `should return null when token is signed with different key`() {
        val otherService = JwtService(
            jwtSecret = "another-different-super-secret-key-that-is-256-bits-0987654321",
            expirationMs = 3600000L
        )
        val token = otherService.generateToken("user-999", "other@example.com", "Other User")

        val claims = jwtService.validateAndExtractClaims(token)
        assertNull(claims)
    }
}
