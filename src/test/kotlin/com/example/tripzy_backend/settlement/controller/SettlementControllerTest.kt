package com.example.tripzy_backend.settlement.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.settlement.dto.RecordSettlementRequest
import com.example.tripzy_backend.settlement.dto.SettlementDto
import com.example.tripzy_backend.settlement.model.PaymentMethod
import com.example.tripzy_backend.settlement.service.SettlementService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.Instant

class SettlementControllerTest {

    private val settlementService: SettlementService = mockk()
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
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
        mockMvc = MockMvcBuilders.standaloneSetup(SettlementController(settlementService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST settlements should record settlement and return 201 Created`() {
        val request = RecordSettlementRequest(
            toUserId = "user-456",
            amount = 3000L,
            paymentMethod = PaymentMethod.BKASH,
            referenceId = "TRX12345"
        )
        val dto = SettlementDto(
            id = "s-1",
            tourId = "t1",
            fromUserId = "user-123",
            fromUserName = "Test User",
            fromUserAvatarUrl = null,
            toUserId = "user-456",
            toUserName = "Creditor User",
            toUserAvatarUrl = null,
            amount = 3000L,
            paymentMethod = PaymentMethod.BKASH,
            notes = null,
            referenceId = "TRX12345",
            settledAt = Instant.now(),
            createdBy = "user-123",
            createdAt = Instant.now()
        )

        every { settlementService.recordSettlement("user-123", "t1", any()) } returns dto

        mockMvc.perform(
            post("/tours/t1/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("s-1"))
            .andExpect(jsonPath("$.data.amount").value(3000))
            .andExpect(jsonPath("$.data.paymentMethod").value("BKASH"))
            .andExpect(jsonPath("$.data.referenceId").value("TRX12345"))
    }

    @Test
    fun `GET settlements should return settlement history`() {
        val dto = SettlementDto(
            id = "s-1",
            tourId = "t1",
            fromUserId = "user-123",
            fromUserName = "Test User",
            fromUserAvatarUrl = null,
            toUserId = "user-456",
            toUserName = "Creditor User",
            toUserAvatarUrl = null,
            amount = 3000L,
            paymentMethod = PaymentMethod.CASH,
            notes = null,
            referenceId = null,
            settledAt = Instant.now(),
            createdBy = "user-123",
            createdAt = Instant.now()
        )

        every { settlementService.getTourSettlements("user-123", "t1") } returns listOf(dto)

        mockMvc.perform(get("/tours/t1/settlements").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("s-1"))
            .andExpect(jsonPath("$.data[0].amount").value(3000))
    }
}
