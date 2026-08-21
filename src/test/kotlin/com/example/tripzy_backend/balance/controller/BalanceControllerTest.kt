package com.example.tripzy_backend.balance.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.balance.dto.SuggestedTransferDetailDto
import com.example.tripzy_backend.balance.dto.TourBalanceResponseDto
import com.example.tripzy_backend.balance.dto.UserBalanceDetailDto
import com.example.tripzy_backend.balance.service.BalanceService
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class BalanceControllerTest {

    private val balanceService: BalanceService = mockk()
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
        mockMvc = MockMvcBuilders.standaloneSetup(BalanceController(balanceService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `GET tour balance should return summary and suggested transfers`() {
        val userBalance = UserBalanceDetailDto(
            userId = "user-123",
            name = "Test User",
            email = "user@example.com",
            avatarUrl = null,
            totalPaid = 6000L,
            totalResponsible = 3000L,
            personalSpend = 0L,
            sharedPaid = 6000L,
            sharedResponsible = 3000L,
            netBalance = 3000L
        )

        val transfer = SuggestedTransferDetailDto(
            fromUserId = "u2",
            fromUserName = "Debtor User",
            toUserId = "user-123",
            toUserName = "Test User",
            amount = 3000L
        )

        val responseDto = TourBalanceResponseDto(
            tourId = "t1",
            totalCost = 6000L,
            totalSharedCost = 6000L,
            totalPersonalCost = 0L,
            myBalance = userBalance,
            memberBalances = listOf(userBalance),
            suggestedTransfers = listOf(transfer)
        )

        every { balanceService.getTourBalance("user-123", "t1") } returns responseDto

        mockMvc.perform(get("/tours/t1/balance").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tourId").value("t1"))
            .andExpect(jsonPath("$.data.totalCost").value(6000))
            .andExpect(jsonPath("$.data.myBalance.netBalance").value(3000))
            .andExpect(jsonPath("$.data.suggestedTransfers[0].amount").value(3000))
            .andExpect(jsonPath("$.data.suggestedTransfers[0].fromUserName").value("Debtor User"))
    }
}
