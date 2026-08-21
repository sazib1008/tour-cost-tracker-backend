package com.example.tripzy_backend.expense.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.expense.dto.CreateExpenseCommentRequest
import com.example.tripzy_backend.expense.dto.ExpenseCommentDto
import com.example.tripzy_backend.expense.service.ExpenseCommentService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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

class ExpenseCommentControllerTest {

    private val expenseCommentService: ExpenseCommentService = mockk()
    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
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
        mockMvc = MockMvcBuilders.standaloneSetup(ExpenseCommentController(expenseCommentService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST comment should create comment successfully`() {
        val request = CreateExpenseCommentRequest(text = "Did we include tips?")
        val commentDto = ExpenseCommentDto(
            id = "c1",
            expenseId = "exp-1",
            tourId = "t1",
            userId = "user-123",
            userName = "Test User",
            userAvatarUrl = null,
            text = "Did we include tips?",
            isSystemGenerated = false,
            createdAt = Instant.now()
        )

        every { expenseCommentService.addComment("user-123", "t1", "exp-1", request) } returns commentDto

        mockMvc.perform(
            post("/tours/t1/expenses/exp-1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("c1"))
            .andExpect(jsonPath("$.data.text").value("Did we include tips?"))
    }

    @Test
    fun `GET comments should return comment thread`() {
        val commentDto = ExpenseCommentDto(
            id = "c1",
            expenseId = "exp-1",
            tourId = "t1",
            userId = null,
            userName = null,
            userAvatarUrl = null,
            text = "Expense was edited by Sazib",
            isSystemGenerated = true,
            createdAt = Instant.now()
        )

        every { expenseCommentService.getComments("user-123", "t1", "exp-1") } returns listOf(commentDto)

        mockMvc.perform(get("/tours/t1/expenses/exp-1/comments").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].isSystemGenerated").value(true))
            .andExpect(jsonPath("$.data[0].text").value("Expense was edited by Sazib"))
    }
}
