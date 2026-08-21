package com.example.tripzy_backend.expense.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "expense_comments")
data class ExpenseComment(
    @Id
    var id: String? = null,

    @Indexed
    var expenseId: String,

    @Indexed
    var tourId: String,

    var userId: String? = null,

    var text: String,

    var isSystemGenerated: Boolean = false,

    @CreatedDate
    var createdAt: Instant = Instant.now()
)
