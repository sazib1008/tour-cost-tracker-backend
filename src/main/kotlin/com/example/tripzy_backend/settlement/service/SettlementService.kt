package com.example.tripzy_backend.settlement.service

import com.example.tripzy_backend.balance.service.DebtSimplificationCalculator
import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.InvalidExpenseException
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.settlement.dto.RecordSettlementRequest
import com.example.tripzy_backend.settlement.dto.SettlementDto
import com.example.tripzy_backend.settlement.model.Settlement
import com.example.tripzy_backend.settlement.repository.SettlementRepository
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val expenseRepository: ExpenseRepository,
    private val tourRepository: TourRepository,
    private val tourMemberRepository: TourMemberRepository,
    private val tourService: TourService,
    private val userRepository: UserRepository,
    private val calculator: DebtSimplificationCalculator,
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher
) {

    @Transactional
    fun recordSettlement(
        currentUserId: String,
        tourId: String,
        request: RecordSettlementRequest
    ): SettlementDto {
        val tour = tourService.assertTourActive(tourId)
        tourService.assertUserIsMember(currentUserId, tourId)

        if (currentUserId == request.toUserId) {
            throw InvalidExpenseException("Payee toUserId cannot be the same as the paying user")
        }

        if (!tourMemberRepository.existsByTourIdAndUserId(tourId, request.toUserId)) {
            throw InvalidExpenseException("Payee user '${request.toUserId}' is not a member of this tour")
        }

        val settlement = Settlement(
            tourId = tourId,
            fromUserId = currentUserId,
            toUserId = request.toUserId,
            amount = request.amount,
            paymentMethod = request.paymentMethod,
            notes = request.notes?.trim(),
            referenceId = request.referenceId?.trim(),
            settledAt = request.settledAt ?: Instant.now(),
            createdBy = currentUserId
        )

        val saved = settlementRepository.save(settlement)

        // Check if tour should transition to SETTLED
        val members = tourMemberRepository.findByTourId(tourId)
        val memberUserIds = members.map { it.userId }.toSet()
        val allExpenses = expenseRepository.findByTourIdOrderByExpenseDateDesc(tourId)
        val allSettlements = settlementRepository.findByTourIdOrderBySettledAtDesc(tourId)

        val summary = calculator.calculate(tourId, memberUserIds, allExpenses, allSettlements)
        val isAllZeroBalances = summary.balances.values.all { it.isSettled() }

        if (isAllZeroBalances && tour.status == TourStatus.ACTIVE) {
            tour.status = TourStatus.SETTLED
            tourRepository.save(tour)
        }

        eventPublisher.publishEvent(
            com.example.tripzy_backend.notification.event.SettlementRecordedEvent(
                tourId = tourId,
                settlementId = saved.id ?: "",
                fromUserId = currentUserId,
                toUserId = request.toUserId,
                amount = saved.amount,
                paymentMethod = saved.paymentMethod
            )
        )

        val fromUser = userRepository.findById(currentUserId).orElse(null)
        val toUser = userRepository.findById(request.toUserId).orElse(null)

        return SettlementDto.fromEntity(
            settlement = saved,
            fromUserName = fromUser?.name ?: "Unknown User",
            fromUserAvatarUrl = fromUser?.avatarUrl,
            toUserName = toUser?.name ?: "Unknown User",
            toUserAvatarUrl = toUser?.avatarUrl
        )
    }

    fun getTourSettlements(userId: String, tourId: String): List<SettlementDto> {
        tourService.assertUserIsMember(userId, tourId)
        val settlements = settlementRepository.findByTourIdOrderBySettledAtDesc(tourId)

        val allUserIds = (settlements.map { it.fromUserId } + settlements.map { it.toUserId }).distinct()
        val userMap = userRepository.findAllById(allUserIds).associateBy { it.id }

        return settlements.map { settlement ->
            val fromUser = userMap[settlement.fromUserId]
            val toUser = userMap[settlement.toUserId]
            SettlementDto.fromEntity(
                settlement = settlement,
                fromUserName = fromUser?.name ?: "Unknown User",
                fromUserAvatarUrl = fromUser?.avatarUrl,
                toUserName = toUser?.name ?: "Unknown User",
                toUserAvatarUrl = toUser?.avatarUrl
            )
        }
    }
}
