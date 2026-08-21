package com.example.tripzy_backend.expense.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.expense.dto.CreateExpenseRequest
import com.example.tripzy_backend.expense.dto.ExpenseDto
import com.example.tripzy_backend.expense.dto.UpdateExpenseRequest
import com.example.tripzy_backend.expense.service.ExpenseService
import com.example.tripzy_backend.idempotency.service.IdempotencyService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours/{tourId}/expenses")
class ExpenseController(
    private val expenseService: ExpenseService,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper
) {

    @PostMapping
    fun createExpense(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody request: CreateExpenseRequest
    ): ResponseEntity<ApiResponse<ExpenseDto>> {
        if (!idempotencyKey.isNullOrBlank()) {
            val requestHash = idempotencyService.hashRequest(request)
            val cachedJson = idempotencyService.findExisting(idempotencyKey, tourId, requestHash)
            if (cachedJson != null) {
                val cachedResponse = objectMapper.readValue(
                    cachedJson,
                    object : TypeReference<ApiResponse<ExpenseDto>>() {}
                )
                return ResponseEntity.ok(cachedResponse)
            }

            val createdExpense = expenseService.createExpense(principal.id, tourId, request)
            val responseEnvelope = ApiResponse.success(data = createdExpense, message = "Expense created successfully")
            idempotencyService.saveResponse(idempotencyKey, tourId, requestHash, responseEnvelope, HttpStatus.CREATED.value())
            return ResponseEntity.status(HttpStatus.CREATED).body(responseEnvelope)
        }

        val createdExpense = expenseService.createExpense(principal.id, tourId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data = createdExpense, message = "Expense created successfully"))
    }

    @GetMapping
    fun getTourExpenses(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String
    ): ResponseEntity<ApiResponse<List<ExpenseDto>>> {
        val expenses = expenseService.getTourExpenses(principal.id, tourId)
        return ResponseEntity.ok(ApiResponse.success(expenses))
    }

    @GetMapping("/{expenseId}")
    fun getExpenseDetails(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @PathVariable expenseId: String
    ): ResponseEntity<ApiResponse<ExpenseDto>> {
        val expense = expenseService.getExpenseDetails(principal.id, tourId, expenseId)
        return ResponseEntity.ok(ApiResponse.success(expense))
    }

    @PutMapping("/{expenseId}")
    fun updateExpense(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @PathVariable expenseId: String,
        @Valid @RequestBody request: UpdateExpenseRequest
    ): ResponseEntity<ApiResponse<ExpenseDto>> {
        val updated = expenseService.updateExpense(principal.id, tourId, expenseId, request)
        return ResponseEntity.ok(ApiResponse.success(data = updated, message = "Expense updated successfully"))
    }

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @PathVariable expenseId: String
    ): ResponseEntity<ApiResponse<Unit>> {
        expenseService.deleteExpense(principal.id, tourId, expenseId)
        return ResponseEntity.ok(ApiResponse.successMessage("Expense deleted successfully"))
    }
}
