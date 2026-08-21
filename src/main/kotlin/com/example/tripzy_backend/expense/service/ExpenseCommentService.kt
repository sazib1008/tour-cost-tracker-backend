package com.example.tripzy_backend.expense.service

import com.example.tripzy_backend.common.exception.ResourceNotFoundException
import com.example.tripzy_backend.expense.dto.CreateExpenseCommentRequest
import com.example.tripzy_backend.expense.dto.ExpenseCommentDto
import com.example.tripzy_backend.expense.model.ExpenseComment
import com.example.tripzy_backend.expense.repository.ExpenseCommentRepository
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ExpenseCommentService(
    private val expenseCommentRepository: ExpenseCommentRepository,
    private val expenseRepository: ExpenseRepository,
    private val tourService: TourService,
    private val userRepository: UserRepository
) {

    @Transactional
    fun addComment(
        userId: String,
        tourId: String,
        expenseId: String,
        request: CreateExpenseCommentRequest
    ): ExpenseCommentDto {
        tourService.assertTourActive(tourId)
        tourService.assertUserIsMember(userId, tourId)

        val expense = expenseRepository.findById(expenseId).orElseThrow {
            ResourceNotFoundException("Expense not found with id: $expenseId")
        }
        if (expense.tourId != tourId) {
            throw ResourceNotFoundException("Expense does not belong to tour: $tourId")
        }

        val comment = ExpenseComment(
            expenseId = expenseId,
            tourId = tourId,
            userId = userId,
            text = request.text.trim(),
            isSystemGenerated = false,
            createdAt = Instant.now()
        )
        val saved = expenseCommentRepository.save(comment)
        val user = userRepository.findById(userId).orElse(null)

        return ExpenseCommentDto.fromEntity(saved, user?.name, user?.avatarUrl)
    }

    @Transactional
    fun addSystemComment(
        tourId: String,
        expenseId: String,
        text: String
    ): ExpenseComment {
        val comment = ExpenseComment(
            expenseId = expenseId,
            tourId = tourId,
            userId = null,
            text = text,
            isSystemGenerated = true,
            createdAt = Instant.now()
        )
        return expenseCommentRepository.save(comment)
    }

    fun getComments(userId: String, tourId: String, expenseId: String): List<ExpenseCommentDto> {
        tourService.assertUserIsMember(userId, tourId)

        val comments = expenseCommentRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId)
        val userIds = comments.mapNotNull { it.userId }.distinct()
        val userMap = userRepository.findAllById(userIds).associateBy { it.id }

        return comments.map { comment ->
            val user = comment.userId?.let { userMap[it] }
            ExpenseCommentDto.fromEntity(comment, user?.name, user?.avatarUrl)
        }
    }
}
