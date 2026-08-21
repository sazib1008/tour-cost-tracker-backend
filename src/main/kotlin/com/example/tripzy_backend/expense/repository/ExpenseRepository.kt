package com.example.tripzy_backend.expense.repository

import com.example.tripzy_backend.expense.model.Expense
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : MongoRepository<Expense, String> {
    fun findByTourIdOrderByExpenseDateDesc(tourId: String): List<Expense>
    fun findByTourIdAndIsPersonal(tourId: String, isPersonal: Boolean): List<Expense>
    fun deleteByTourId(tourId: String)
    fun countByTourId(tourId: String): Long
}
