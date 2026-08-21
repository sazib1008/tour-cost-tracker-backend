package com.example.tripzy_backend.auth.controller

import com.example.tripzy_backend.auth.dto.AuthResponse
import com.example.tripzy_backend.auth.dto.GoogleAuthRequest
import com.example.tripzy_backend.auth.service.AuthService
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.user.dto.UserDto
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {

    private val authService: AuthService = mockk()
    private val objectMapper = ObjectMapper()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST auth google should return 200 with JWT and user`() {
        val request = GoogleAuthRequest(idToken = "valid-token")
        val authResponse = AuthResponse(
            token = "jwt.sample.token",
            user = UserDto(id = "u1", email = "test@example.com", name = "Test User", avatarUrl = null),
            expiresInMs = 3600000L
        )

        every { authService.authenticateWithGoogle(request) } returns authResponse

        mockMvc.perform(
            post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value("jwt.sample.token"))
            .andExpect(jsonPath("$.data.user.id").value("u1"))
            .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
    }

    @Test
    fun `POST auth google with blank idToken should return 400 bad request`() {
        val request = mapOf("idToken" to "")

        mockMvc.perform(
            post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }
}
