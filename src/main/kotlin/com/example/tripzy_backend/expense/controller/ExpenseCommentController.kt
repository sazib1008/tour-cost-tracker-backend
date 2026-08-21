package com.example.tripzy_backend.expense.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.expense.dto.CreateExpenseCommentRequest
import com.example.tripzy_backend.expense.dto.ExpenseCommentDto
import com.example.tripzy_backend.expense.service.ExpenseCommentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours/{tourId}/expenses/{expenseId}/comments")
class ExpenseCommentController(
    private val expenseCommentService: ExpenseCommentService
) {

    @PostMapping
    fun addComment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @PathVariable expenseId: String,
        @Valid @RequestBody request: CreateExpenseCommentRequest
    ): ResponseEntity<ApiResponse<ExpenseCommentDto>> {
        val comment = expenseCommentService.addComment(principal.id, tourId, expenseId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data = comment, message = "Comment added successfully"))
    }

    @GetMapping
    fun getComments(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @PathVariable expenseId: String
    ): ResponseEntity<ApiResponse<List<ExpenseCommentDto>>> {
        val comments = expenseCommentService.getComments(principal.id, tourId, expenseId)
        return ResponseEntity.ok(ApiResponse.success(comments))
    }
}
