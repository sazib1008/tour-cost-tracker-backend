package com.example.tripzy_backend.balance.service

import com.example.tripzy_backend.balance.dto.SuggestedTransferDetailDto
import com.example.tripzy_backend.balance.dto.TourBalanceResponseDto
import com.example.tripzy_backend.balance.dto.UserBalanceDetailDto
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.settlement.repository.SettlementRepository
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class BalanceService(
    private val tourService: TourService,
    private val tourMemberRepository: TourMemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val userRepository: UserRepository,
    private val calculator: DebtSimplificationCalculator
) {

    fun getTourBalance(userId: String, tourId: String): TourBalanceResponseDto {
        tourService.assertUserIsMember(userId, tourId)

        val members = tourMemberRepository.findByTourId(tourId)
        val memberUserIds = members.map { it.userId }.toSet()
        val expenses = expenseRepository.findByTourIdOrderByExpenseDateDesc(tourId)
        val settlements = settlementRepository.findByTourIdOrderBySettledAtDesc(tourId)

        val summary = calculator.calculate(tourId, memberUserIds, expenses, settlements)

        val allInvolvedUserIds = (memberUserIds + summary.balances.keys).distinct()
        val users = userRepository.findAllById(allInvolvedUserIds).associateBy { it.id }

        val memberBalances = summary.balances.values.map { balance ->
            val u = users[balance.userId]
            UserBalanceDetailDto(
                userId = balance.userId,
                name = u?.name ?: "Unknown User",
                email = u?.email ?: "",
                avatarUrl = u?.avatarUrl,
                totalPaid = balance.totalPaid,
                totalResponsible = balance.totalResponsible,
                personalSpend = balance.personalSpend,
                sharedPaid = balance.sharedPaid,
                sharedResponsible = balance.sharedResponsible,
                netBalance = balance.netBalance
            )
        }

        val myBalance = memberBalances.find { it.userId == userId }

        val suggestedTransfers = summary.suggestedTransfers.map { transfer ->
            val fromUser = users[transfer.fromUserId]
            val toUser = users[transfer.toUserId]
            SuggestedTransferDetailDto(
                fromUserId = transfer.fromUserId,
                fromUserName = fromUser?.name ?: "Unknown User",
                toUserId = transfer.toUserId,
                toUserName = toUser?.name ?: "Unknown User",
                amount = transfer.amount
            )
        }

        return TourBalanceResponseDto(
            tourId = tourId,
            totalCost = summary.totalCost,
            totalSharedCost = summary.totalSharedCost,
            totalPersonalCost = summary.totalPersonalCost,
            myBalance = myBalance,
            memberBalances = memberBalances,
            suggestedTransfers = suggestedTransfers
        )
    }
}
