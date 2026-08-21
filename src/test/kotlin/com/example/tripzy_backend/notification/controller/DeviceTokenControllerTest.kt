package com.example.tripzy_backend.notification.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.notification.dto.RegisterDeviceTokenRequest
import com.example.tripzy_backend.notification.service.DeviceTokenService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class DeviceTokenControllerTest {

    private val deviceTokenService: DeviceTokenService = mockk()
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
        mockMvc = MockMvcBuilders.standaloneSetup(DeviceTokenController(deviceTokenService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST device tokens should register token successfully`() {
        val request = RegisterDeviceTokenRequest(token = "fcm-sample-token-123")
        every { deviceTokenService.registerToken("user-123", request) } just runs

        mockMvc.perform(
            post("/users/me/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `DELETE device tokens should remove token`() {
        every { deviceTokenService.removeToken("user-123", "fcm-sample-token-123") } just runs

        mockMvc.perform(delete("/users/me/device-tokens/fcm-sample-token-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }
}
