package com.example.tripzy_backend.auth.service

import com.example.tripzy_backend.auth.dto.GoogleAuthRequest
import com.example.tripzy_backend.auth.dto.GoogleTokenPayload
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.service.UserService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthServiceTest {

    private val googleTokenVerifierService: GoogleTokenVerifierService = mockk()
    private val userService: UserService = mockk()
    private val jwtService: JwtService = mockk()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(googleTokenVerifierService, userService, jwtService)
    }

    @Test
    fun `authenticateWithGoogle should verify token, find-or-create user and issue JWT`() {
        val idToken = "valid-google-id-token"
        val payload = GoogleTokenPayload(
            sub = "google-user-123",
            email = "user@example.com",
            name = "Test User",
            picture = "http://pic.jpg"
        )
        val user = User(
            id = "user-db-id-1",
            email = "user@example.com",
            name = "Test User",
            avatarUrl = "http://pic.jpg",
            googleId = "google-user-123"
        )

        every { googleTokenVerifierService.verifyToken(idToken) } returns payload
        every { userService.findOrCreateGoogleUser("google-user-123", "user@example.com", "Test User", "http://pic.jpg") } returns user
        every { jwtService.generateToken("user-db-id-1", "user@example.com", "Test User") } returns "generated-jwt-token"
        every { jwtService.expirationMs } returns 3600000L

        val response = authService.authenticateWithGoogle(GoogleAuthRequest(idToken))

        assertNotNull(response)
        assertEquals("generated-jwt-token", response.token)
        assertEquals("user-db-id-1", response.user.id)
        assertEquals("user@example.com", response.user.email)
        assertEquals("Test User", response.user.name)
    }
}
