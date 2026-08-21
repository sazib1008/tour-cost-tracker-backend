package com.example.tripzy_backend.expense.service

import com.example.tripzy_backend.common.exception.ForbiddenException
import com.example.tripzy_backend.common.exception.InvalidExpenseException
import com.example.tripzy_backend.common.exception.ResourceNotFoundException
import com.example.tripzy_backend.expense.dto.CreateExpenseRequest
import com.example.tripzy_backend.expense.dto.ExpenseDto
import com.example.tripzy_backend.expense.dto.UpdateExpenseRequest
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.expense.model.SplitType
import com.example.tripzy_backend.expense.repository.ExpenseCommentRepository
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseCommentRepository: ExpenseCommentRepository,
    private val tourMemberRepository: TourMemberRepository,
    private val tourService: TourService,
    private val userRepository: UserRepository,
    private val splitCalculator: ExpenseSplitCalculator,
    private val expenseCommentService: ExpenseCommentService,
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher
) {

    @Transactional
    fun createExpense(
        userId: String,
        tourId: String,
        request: CreateExpenseRequest
    ): ExpenseDto {
        val tour = tourService.assertTourActive(tourId)
        tourService.assertUserIsMember(userId, tourId)

        val payments = resolvePayments(userId, tourId, request.amount, request.payments?.map { Payment(it.userId, it.amount) })
        val allocations = resolveAllocations(
            userId = userId,
            tourId = tourId,
            totalAmount = request.amount,
            isPersonal = request.isPersonal,
            splitType = request.splitType,
            allocations = request.allocations?.map { Allocation(it.userId, it.amount) },
            participants = request.participants,
            percentages = request.percentages,
            shares = request.shares
        )

        val expense = Expense(
            tourId = tourId,
            title = request.title.trim(),
            description = request.description?.trim(),
            amount = request.amount,
            currency = request.currency.uppercase().ifBlank { tour.baseCurrency },
            baseCurrencyAmount = request.amount,
            exchangeRate = 1.0,
            category = request.category,
            splitType = request.splitType,
            isPersonal = request.isPersonal,
            expenseDate = request.expenseDate ?: Instant.now(),
            payments = payments,
            allocations = allocations,
            createdBy = userId,
            receiptUrl = request.receiptUrl
        )

        try {
            expense.validateInvariants()
        } catch (e: IllegalArgumentException) {
            throw InvalidExpenseException(e.message ?: "Invalid expense amounts")
        }

        val saved = expenseRepository.save(expense)

        eventPublisher.publishEvent(
            com.example.tripzy_backend.notification.event.ExpenseCreatedEvent(
                tourId = tourId,
                expenseId = saved.id ?: "",
                title = saved.title,
                amount = saved.amount,
                currency = saved.currency,
                createdByUserId = userId
            )
        )

        return ExpenseDto.fromEntity(saved)
    }

    fun getTourExpenses(userId: String, tourId: String): List<ExpenseDto> {
        tourService.assertUserIsMember(userId, tourId)
        val expenses = expenseRepository.findByTourIdOrderByExpenseDateDesc(tourId)
        return expenses.map { ExpenseDto.fromEntity(it) }
    }

    fun getExpenseDetails(userId: String, tourId: String, expenseId: String): ExpenseDto {
        tourService.assertUserIsMember(userId, tourId)
        val expense = findExpenseByIdAndTourId(expenseId, tourId)
        return ExpenseDto.fromEntity(expense)
    }

    @Transactional
    fun updateExpense(
        userId: String,
        tourId: String,
        expenseId: String,
        request: UpdateExpenseRequest
    ): ExpenseDto {
        tourService.assertTourActive(tourId)
        val membership = tourService.assertUserIsMember(userId, tourId)
        val expense = findExpenseByIdAndTourId(expenseId, tourId)

        if (expense.createdBy != userId && membership.role != TourRole.ADMIN) {
            throw ForbiddenException("Only the expense creator or tour admin can edit this expense")
        }

        val payments = resolvePayments(userId, tourId, request.amount, request.payments?.map { Payment(it.userId, it.amount) })
        val allocations = resolveAllocations(
            userId = userId,
            tourId = tourId,
            totalAmount = request.amount,
            isPersonal = request.isPersonal,
            splitType = request.splitType,
            allocations = request.allocations?.map { Allocation(it.userId, it.amount) },
            participants = request.participants,
            percentages = request.percentages,
            shares = request.shares
        )

        expense.title = request.title.trim()
        expense.description = request.description?.trim()
        expense.amount = request.amount
        expense.baseCurrencyAmount = request.amount
        expense.currency = request.currency.uppercase().ifBlank { expense.currency }
        expense.category = request.category
        expense.splitType = request.splitType
        expense.isPersonal = request.isPersonal
        request.expenseDate?.let { expense.expenseDate = it }
        expense.receiptUrl = request.receiptUrl
        expense.payments = payments
        expense.allocations = allocations

        try {
            expense.validateInvariants()
        } catch (e: IllegalArgumentException) {
            throw InvalidExpenseException(e.message ?: "Invalid expense amounts")
        }

        val saved = expenseRepository.save(expense)

        val editor = userRepository.findById(userId).orElse(null)
        val editorName = editor?.name ?: "A member"
        val timestampStr = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC))
        expenseCommentService.addSystemComment(
            tourId = tourId,
            expenseId = expenseId,
            text = "Expense was edited by $editorName on $timestampStr"
        )

        return ExpenseDto.fromEntity(saved)
    }

    @Transactional
    fun deleteExpense(userId: String, tourId: String, expenseId: String) {
        tourService.assertTourActive(tourId)
        val membership = tourService.assertUserIsMember(userId, tourId)
        val expense = findExpenseByIdAndTourId(expenseId, tourId)

        if (expense.createdBy != userId && membership.role != TourRole.ADMIN) {
            throw ForbiddenException("Only the expense creator or tour admin can delete this expense")
        }

        expenseRepository.deleteById(expenseId)
        expenseCommentRepository.deleteByExpenseId(expenseId)
    }

    private fun findExpenseByIdAndTourId(expenseId: String, tourId: String): Expense {
        val expense = expenseRepository.findById(expenseId).orElseThrow {
            ResourceNotFoundException("Expense not found with id: $expenseId")
        }
        if (expense.tourId != tourId) {
            throw ResourceNotFoundException("Expense does not belong to tour: $tourId")
        }
        return expense
    }

    private fun resolvePayments(
        currentUserId: String,
        tourId: String,
        totalAmount: Long,
        payments: List<Payment>?
    ): List<Payment> {
        if (payments.isNullOrEmpty()) {
            return listOf(Payment(userId = currentUserId, amount = totalAmount))
        }

        payments.forEach { payment ->
            if (!tourMemberRepository.existsByTourIdAndUserId(tourId, payment.userId)) {
                throw InvalidExpenseException("Payer '${payment.userId}' is not a member of this tour")
            }
        }
        return payments
    }

    private fun resolveAllocations(
        userId: String,
        tourId: String,
        totalAmount: Long,
        isPersonal: Boolean,
        splitType: SplitType,
        allocations: List<Allocation>?,
        participants: List<String>?,
        percentages: Map<String, Double>?,
        shares: Map<String, Int>?
    ): List<Allocation> {
        if (isPersonal) {
            return listOf(Allocation(userId = userId, amount = totalAmount))
        }

        return when (splitType) {
            SplitType.EQUAL -> {
                val candidateUserIds = if (!participants.isNullOrEmpty()) {
                    participants
                } else {
                    tourMemberRepository.findByTourId(tourId).map { it.userId }
                }

                candidateUserIds.forEach { participantId ->
                    if (!tourMemberRepository.existsByTourIdAndUserId(tourId, participantId)) {
                        throw InvalidExpenseException("Participant '$participantId' is not a member of this tour")
                    }
                }
                splitCalculator.calculateEqualSplit(totalAmount, candidateUserIds)
            }
            SplitType.EXACT -> {
                if (allocations.isNullOrEmpty()) {
                    throw InvalidExpenseException("Allocations list cannot be empty for EXACT split")
                }
                allocations.forEach { alloc ->
                    if (!tourMemberRepository.existsByTourIdAndUserId(tourId, alloc.userId)) {
                        throw InvalidExpenseException("Participant '${alloc.userId}' is not a member of this tour")
                    }
                }
                val exactMap = allocations.associate { it.userId to it.amount }
                splitCalculator.calculateExactSplit(totalAmount, exactMap)
            }
            SplitType.PERCENTAGE -> {
                if (percentages.isNullOrEmpty()) {
                    throw InvalidExpenseException("Percentages map cannot be empty for PERCENTAGE split")
                }
                percentages.keys.forEach { participantId ->
                    if (!tourMemberRepository.existsByTourIdAndUserId(tourId, participantId)) {
                        throw InvalidExpenseException("Participant '$participantId' is not a member of this tour")
                    }
                }
                splitCalculator.calculatePercentageSplit(totalAmount, percentages)
            }
            SplitType.SHARES -> {
                if (shares.isNullOrEmpty()) {
                    throw InvalidExpenseException("Shares map cannot be empty for SHARES split")
                }
                shares.keys.forEach { participantId ->
                    if (!tourMemberRepository.existsByTourIdAndUserId(tourId, participantId)) {
                        throw InvalidExpenseException("Participant '$participantId' is not a member of this tour")
                    }
                }
                splitCalculator.calculateSharesSplit(totalAmount, shares)
            }
        }
    }
}
