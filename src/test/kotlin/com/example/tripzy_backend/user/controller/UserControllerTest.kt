package com.example.tripzy_backend.user.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.user.dto.UpdateUserRequest
import com.example.tripzy_backend.user.dto.UserDto
import com.example.tripzy_backend.user.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UserControllerTest {

    private val userService: UserService = mockk()
    private val objectMapper = ObjectMapper()
    private lateinit var mockMvc: MockMvc

    private val mockPrincipal = UserPrincipal(
        id = "user-123",
        email = "user@example.com",
        fullName = "Test User"
    )

    private val principalResolver = object : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java)
        }

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?
        ): Any {
            return mockPrincipal
        }
    }

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(UserController(userService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `GET users me should return current user profile`() {
        val userDto = UserDto(id = "user-123", email = "user@example.com", name = "Test User", avatarUrl = "http://avatar.jpg")
        every { userService.getProfile("user-123") } returns userDto

        mockMvc.perform(get("/users/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("user-123"))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.name").value("Test User"))
    }

    @Test
    fun `PUT users me should update profile`() {
        val request = UpdateUserRequest(name = "Updated Name", avatarUrl = "http://new-avatar.jpg")
        val updatedDto = UserDto(id = "user-123", email = "user@example.com", name = "Updated Name", avatarUrl = "http://new-avatar.jpg")

        every { userService.updateProfile("user-123", request) } returns updatedDto

        mockMvc.perform(
            put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Name"))
            .andExpect(jsonPath("$.data.avatarUrl").value("http://new-avatar.jpg"))
    }

    @Test
    fun `GET users by id should return user profile`() {
        val userDto = UserDto(id = "other-user", email = "other@example.com", name = "Other User", avatarUrl = null)
        every { userService.getProfile("other-user") } returns userDto

        mockMvc.perform(get("/users/other-user").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("other-user"))
            .andExpect(jsonPath("$.data.name").value("Other User"))
    }
}
