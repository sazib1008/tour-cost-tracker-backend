package com.example.tripzy_backend.expense.service

import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.TourArchivedException
import com.example.tripzy_backend.expense.dto.CreateExpenseRequest
import com.example.tripzy_backend.expense.dto.PaymentDto
import com.example.tripzy_backend.expense.dto.UpdateExpenseRequest
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.ExpenseCategory
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.expense.model.SplitType
import com.example.tripzy_backend.expense.repository.ExpenseCommentRepository
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.tour.model.TourMember
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class ExpenseServiceTest {

    private val expenseRepository: ExpenseRepository = mockk()
    private val expenseCommentRepository: ExpenseCommentRepository = mockk()
    private val tourMemberRepository: TourMemberRepository = mockk()
    private val tourService: TourService = mockk()
    private val userRepository: UserRepository = mockk()
    private val splitCalculator = ExpenseSplitCalculator()
    private val expenseCommentService: ExpenseCommentService = mockk(relaxed = true)
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher = mockk(relaxed = true)

    private lateinit var expenseService: ExpenseService

    @BeforeEach
    fun setUp() {
        expenseService = ExpenseService(
            expenseRepository = expenseRepository,
            expenseCommentRepository = expenseCommentRepository,
            tourMemberRepository = tourMemberRepository,
            tourService = tourService,
            userRepository = userRepository,
            splitCalculator = splitCalculator,
            expenseCommentService = expenseCommentService,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `createExpense should handle shared expense with equal split across all members`() {
        val tour = Tour(id = "tour-1", title = "Trip", baseCurrency = "BDT", inviteCode = "TRIP01", createdBy = "u1")
        val members = listOf(
            TourMember(tourId = "tour-1", userId = "u1", role = TourRole.ADMIN),
            TourMember(tourId = "tour-1", userId = "u2", role = TourRole.MEMBER)
        )

        every { tourService.assertTourActive("tour-1") } returns tour
        every { tourService.assertUserIsMember("u1", "tour-1") } returns members[0]
        every { tourMemberRepository.findByTourId("tour-1") } returns members
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u1") } returns true
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u2") } returns true
        every { expenseRepository.save(any()) } answers {
            val exp = firstArg<Expense>()
            exp.id = "exp-1"
            exp
        }

        val request = CreateExpenseRequest(
            title = "Dinner at Seagull",
            amount = 4000L, // 40 BDT
            category = ExpenseCategory.FOOD,
            splitType = SplitType.EQUAL,
            isPersonal = false
        )

        val result = expenseService.createExpense("u1", "tour-1", request)

        assertNotNull(result)
        assertEquals("exp-1", result.id)
        assertEquals(4000L, result.amount)
        assertEquals(1, result.payments.size)
        assertEquals("u1", result.payments[0].userId)
        assertEquals(4000L, result.payments[0].amount)

        assertEquals(2, result.allocations.size)
        assertEquals(2000L, result.allocations[0].amount)
        assertEquals(2000L, result.allocations[1].amount)
    }

    @Test
    fun `createExpense should handle personal expense allocated 100 percent to creator`() {
        val tour = Tour(id = "tour-1", title = "Trip", baseCurrency = "BDT", inviteCode = "TRIP01", createdBy = "u1")
        val member = TourMember(tourId = "tour-1", userId = "u1", role = TourRole.MEMBER)

        every { tourService.assertTourActive("tour-1") } returns tour
        every { tourService.assertUserIsMember("u1", "tour-1") } returns member
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u1") } returns true
        every { expenseRepository.save(any()) } answers {
            val exp = firstArg<Expense>()
            exp.id = "exp-personal"
            exp
        }

        val request = CreateExpenseRequest(
            title = "Personal Souvenir",
            amount = 1500L,
            category = ExpenseCategory.SHOPPING,
            isPersonal = true
        )

        val result = expenseService.createExpense("u1", "tour-1", request)

        assertTrue(result.isPersonal)
        assertEquals(1, result.allocations.size)
        assertEquals("u1", result.allocations[0].userId)
        assertEquals(1500L, result.allocations[0].amount)
    }

    @Test
    fun `createExpense should support multiple payers`() {
        val tour = Tour(id = "tour-1", title = "Trip", baseCurrency = "BDT", inviteCode = "TRIP01", createdBy = "u1")
        val members = listOf(
            TourMember(tourId = "tour-1", userId = "u1", role = TourRole.ADMIN),
            TourMember(tourId = "tour-1", userId = "u2", role = TourRole.MEMBER)
        )

        every { tourService.assertTourActive("tour-1") } returns tour
        every { tourService.assertUserIsMember("u1", "tour-1") } returns members[0]
        every { tourMemberRepository.findByTourId("tour-1") } returns members
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u1") } returns true
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u2") } returns true
        every { expenseRepository.save(any()) } answers { firstArg() }

        val request = CreateExpenseRequest(
            title = "Hotel Booking",
            amount = 10000L,
            category = ExpenseCategory.ACCOMMODATION,
            payments = listOf(
                PaymentDto("u1", 6000L),
                PaymentDto("u2", 4000L)
            ),
            splitType = SplitType.EQUAL
        )

        val result = expenseService.createExpense("u1", "tour-1", request)

        assertEquals(2, result.payments.size)
        assertEquals(6000L, result.payments[0].amount)
        assertEquals(4000L, result.payments[1].amount)
        assertEquals(10000L, result.payments.sumOf { it.amount })
    }

    @Test
    fun `createExpense should throw TourArchivedException when tour is archived`() {
        every { tourService.assertTourActive("tour-archived") } throws TourArchivedException("tour-archived")

        assertThrows(TourArchivedException::class.java) {
            expenseService.createExpense("u1", "tour-archived", CreateExpenseRequest(title = "Item", amount = 1000L))
        }
    }

    @Test
    fun `updateExpense should record system audit comment`() {
        val tour = Tour(id = "tour-1", title = "Trip", baseCurrency = "BDT", inviteCode = "TRIP01", createdBy = "u1")
        val member = TourMember(tourId = "tour-1", userId = "u1", role = TourRole.ADMIN)
        val user = User(id = "u1", email = "u1@example.com", name = "Sazib")
        val existingExpense = Expense(
            id = "exp-1",
            tourId = "tour-1",
            title = "Old Title",
            amount = 2000L,
            createdBy = "u1",
            payments = listOf(Payment("u1", 2000L)),
            allocations = listOf(Allocation("u1", 2000L))
        )

        every { tourService.assertTourActive("tour-1") } returns tour
        every { tourService.assertUserIsMember("u1", "tour-1") } returns member
        every { expenseRepository.findById("exp-1") } returns Optional.of(existingExpense)
        every { tourMemberRepository.existsByTourIdAndUserId("tour-1", "u1") } returns true
        every { tourMemberRepository.findByTourId("tour-1") } returns listOf(member)
        every { expenseRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById("u1") } returns Optional.of(user)

        val updateRequest = UpdateExpenseRequest(
            title = "New Title",
            amount = 2000L,
            isPersonal = true
        )

        expenseService.updateExpense("u1", "tour-1", "exp-1", updateRequest)

        verify {
            expenseCommentService.addSystemComment(
                tourId = "tour-1",
                expenseId = "exp-1",
                text = match { it.contains("Expense was edited by Sazib") }
            )
        }
    }
}
