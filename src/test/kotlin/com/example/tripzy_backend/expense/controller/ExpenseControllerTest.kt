package com.example.tripzy_backend.expense.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.ConflictException
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.expense.dto.CreateExpenseRequest
import com.example.tripzy_backend.expense.dto.ExpenseDto
import com.example.tripzy_backend.expense.model.ExpenseCategory
import com.example.tripzy_backend.expense.model.SplitType
import com.example.tripzy_backend.expense.service.ExpenseService
import com.example.tripzy_backend.idempotency.service.IdempotencyService
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

class ExpenseControllerTest {

    private val expenseService: ExpenseService = mockk()
    private val idempotencyService: IdempotencyService = mockk(relaxed = true)
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
        mockMvc = MockMvcBuilders.standaloneSetup(ExpenseController(expenseService, idempotencyService, objectMapper))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST expenses should create expense successfully`() {
        val request = CreateExpenseRequest(title = "Lunch", amount = 5000L, category = ExpenseCategory.FOOD)
        val expenseDto = ExpenseDto(
            id = "exp-1",
            tourId = "tour-1",
            title = "Lunch",
            description = null,
            amount = 5000L,
            currency = "BDT",
            baseCurrencyAmount = 5000L,
            exchangeRate = 1.0,
            category = ExpenseCategory.FOOD,
            splitType = SplitType.EQUAL,
            isPersonal = false,
            expenseDate = Instant.now(),
            payments = emptyList(),
            allocations = emptyList(),
            createdBy = "user-123",
            receiptUrl = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { expenseService.createExpense("user-123", "tour-1", any()) } returns expenseDto

        mockMvc.perform(
            post("/tours/tour-1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("exp-1"))
            .andExpect(jsonPath("$.data.amount").value(5000))
    }

    @Test
    fun `POST expenses with idempotency key cache hit returns cached response`() {
        val request = CreateExpenseRequest(title = "Lunch", amount = 5000L)
        val cachedDto = ExpenseDto(
            id = "exp-cached-1",
            tourId = "tour-1",
            title = "Lunch",
            description = null,
            amount = 5000L,
            currency = "BDT",
            baseCurrencyAmount = 5000L,
            exchangeRate = 1.0,
            category = ExpenseCategory.FOOD,
            splitType = SplitType.EQUAL,
            isPersonal = false,
            expenseDate = Instant.now(),
            payments = emptyList(),
            allocations = emptyList(),
            createdBy = "user-123",
            receiptUrl = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val cachedResponse = ApiResponse.success(data = cachedDto, message = "Expense created successfully")
        val cachedResponseJson = objectMapper.writeValueAsString(cachedResponse)

        every { idempotencyService.hashRequest(any()) } returns "hash123"
        every { idempotencyService.findExisting("idem-uuid-1", "tour-1", "hash123") } returns cachedResponseJson

        mockMvc.perform(
            post("/tours/tour-1/expenses")
                .header("Idempotency-Key", "idem-uuid-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value("exp-cached-1"))
    }

    @Test
    fun `POST expenses with idempotency key collision with different body returns 409`() {
        val request = CreateExpenseRequest(title = "Lunch", amount = 5000L)

        every { idempotencyService.hashRequest(any()) } returns "hash-different"
        every { idempotencyService.findExisting("idem-uuid-1", "tour-1", "hash-different") } throws ConflictException("Idempotency key mismatch")

        mockMvc.perform(
            post("/tours/tour-1/expenses")
                .header("Idempotency-Key", "idem-uuid-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
    }

    @Test
    fun `GET expenses should return list of tour expenses`() {
        val expenseDto = ExpenseDto(
            id = "exp-1",
            tourId = "tour-1",
            title = "Lunch",
            description = null,
            amount = 5000L,
            currency = "BDT",
            baseCurrencyAmount = 5000L,
            exchangeRate = 1.0,
            category = ExpenseCategory.FOOD,
            splitType = SplitType.EQUAL,
            isPersonal = false,
            expenseDate = Instant.now(),
            payments = emptyList(),
            allocations = emptyList(),
            createdBy = "user-123",
            receiptUrl = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { expenseService.getTourExpenses("user-123", "tour-1") } returns listOf(expenseDto)

        mockMvc.perform(get("/tours/tour-1/expenses").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("exp-1"))
    }
}
