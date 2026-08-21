package com.example.tripzy_backend.expense.repository

import com.example.tripzy_backend.expense.model.ExpenseComment
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpenseCommentRepository : MongoRepository<ExpenseComment, String> {
    fun findByExpenseIdOrderByCreatedAtAsc(expenseId: String): List<ExpenseComment>
    fun deleteByExpenseId(expenseId: String)
    fun deleteByTourId(tourId: String)
}
