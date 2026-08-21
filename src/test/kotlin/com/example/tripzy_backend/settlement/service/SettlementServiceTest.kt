package com.example.tripzy_backend.settlement.service

import com.example.tripzy_backend.balance.model.TourBalanceSummary
import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.balance.service.DebtSimplificationCalculator
import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.InvalidExpenseException
import com.example.tripzy_backend.common.exception.TourArchivedException
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.settlement.dto.RecordSettlementRequest
import com.example.tripzy_backend.settlement.model.PaymentMethod
import com.example.tripzy_backend.settlement.model.Settlement
import com.example.tripzy_backend.settlement.repository.SettlementRepository
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.tour.model.TourMember
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class SettlementServiceTest {

    private val settlementRepository: SettlementRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val tourRepository: TourRepository = mockk()
    private val tourMemberRepository: TourMemberRepository = mockk()
    private val tourService: TourService = mockk()
    private val userRepository: UserRepository = mockk()
    private val calculator: DebtSimplificationCalculator = mockk()
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher = mockk(relaxed = true)

    private lateinit var settlementService: SettlementService

    @BeforeEach
    fun setUp() {
        settlementService = SettlementService(
            settlementRepository = settlementRepository,
            expenseRepository = expenseRepository,
            tourRepository = tourRepository,
            tourMemberRepository = tourMemberRepository,
            tourService = tourService,
            userRepository = userRepository,
            calculator = calculator,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `recordSettlement should save settlement and transition tour status to SETTLED when all balances are zero`() {
        val tour = Tour(id = "t1", title = "Trip", status = TourStatus.ACTIVE, inviteCode = "TRIP01", createdBy = "u1")
        val members = listOf(
            TourMember(tourId = "t1", userId = "u1", role = TourRole.ADMIN),
            TourMember(tourId = "t1", userId = "u2", role = TourRole.MEMBER)
        )
        val fromUser = User(id = "u2", email = "u2@example.com", name = "Debtor User")
        val toUser = User(id = "u1", email = "u1@example.com", name = "Creditor User")

        every { tourService.assertTourActive("t1") } returns tour
        every { tourService.assertUserIsMember("u2", "t1") } returns members[1]
        every { tourMemberRepository.existsByTourIdAndUserId("t1", "u1") } returns true
        every { tourMemberRepository.findByTourId("t1") } returns members
        every { expenseRepository.findByTourIdOrderByExpenseDateDesc("t1") } returns emptyList()
        every { settlementRepository.findByTourIdOrderBySettledAtDesc("t1") } returns emptyList()
        every { settlementRepository.save(any()) } answers {
            val s = firstArg<Settlement>()
            s.id = "s-1"
            s
        }
        every { tourRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById("u2") } returns Optional.of(fromUser)
        every { userRepository.findById("u1") } returns Optional.of(toUser)

        val zeroSummary = TourBalanceSummary(
            tourId = "t1",
            totalCost = 3000L,
            totalSharedCost = 3000L,
            totalPersonalCost = 0L,
            balances = mapOf(
                "u1" to UserBalance("u1", netBalance = 0L),
                "u2" to UserBalance("u2", netBalance = 0L)
            ),
            suggestedTransfers = emptyList()
        )
        every { calculator.calculate("t1", setOf("u1", "u2"), any(), any()) } returns zeroSummary

        val request = RecordSettlementRequest(
            toUserId = "u1",
            amount = 1500L,
            paymentMethod = PaymentMethod.BKASH,
            referenceId = "TRX998877"
        )

        val result = settlementService.recordSettlement("u2", "t1", request)

        assertNotNull(result)
        assertEquals("s-1", result.id)
        assertEquals("u2", result.fromUserId)
        assertEquals("u1", result.toUserId)
        assertEquals(1500L, result.amount)
        assertEquals(PaymentMethod.BKASH, result.paymentMethod)
        assertEquals("TRX998877", result.referenceId)

        // Tour status updated to SETTLED
        verify { tourRepository.save(match { it.status == TourStatus.SETTLED }) }
    }

    @Test
    fun `recordSettlement should throw InvalidExpenseException if paying self`() {
        every { tourService.assertTourActive("t1") } returns Tour(id = "t1", title = "T", inviteCode = "C", createdBy = "u1")
        every { tourService.assertUserIsMember("u1", "t1") } returns TourMember(tourId = "t1", userId = "u1")

        assertThrows(InvalidExpenseException::class.java) {
            settlementService.recordSettlement("u1", "t1", RecordSettlementRequest(toUserId = "u1", amount = 500L))
        }
    }

    @Test
    fun `recordSettlement should throw TourArchivedException when tour is archived`() {
        every { tourService.assertTourActive("t-archived") } throws TourArchivedException("t-archived")

        assertThrows(TourArchivedException::class.java) {
            settlementService.recordSettlement("u1", "t-archived", RecordSettlementRequest(toUserId = "u2", amount = 500L))
        }
    }
}
