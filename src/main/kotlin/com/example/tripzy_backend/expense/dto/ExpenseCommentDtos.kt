package com.example.tripzy_backend.expense.dto

import com.example.tripzy_backend.expense.model.ExpenseComment
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateExpenseCommentRequest(
    @field:NotBlank(message = "Comment text cannot be blank")
    @field:Size(min = 1, max = 500, message = "Comment must be between 1 and 500 characters")
    val text: String
)

data class ExpenseCommentDto(
    val id: String,
    val expenseId: String,
    val tourId: String,
    val userId: String?,
    val userName: String?,
    val userAvatarUrl: String?,
    val text: String,
    val isSystemGenerated: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun fromEntity(
            comment: ExpenseComment,
            userName: String? = null,
            userAvatarUrl: String? = null
        ): ExpenseCommentDto = ExpenseCommentDto(
            id = comment.id ?: "",
            expenseId = comment.expenseId,
            tourId = comment.tourId,
            userId = comment.userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            text = comment.text,
            isSystemGenerated = comment.isSystemGenerated,
            createdAt = comment.createdAt
        )
    }
}
