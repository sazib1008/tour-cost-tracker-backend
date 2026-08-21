package com.example.tripzy_backend.export.service

import com.example.tripzy_backend.balance.model.TourBalanceSummary
import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.ExpenseCategory
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.expense.model.SplitType
import com.example.tripzy_backend.settlement.model.PaymentMethod
import com.example.tripzy_backend.settlement.model.Settlement
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.user.model.User
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ExportServicesTest {

    private val csvExportService = CsvExportService()
    private val pdfExportService = PdfExportService()

    private val user1 = User(id = "u1", email = "u1@example.com", name = "User One")
    private val user2 = User(id = "u2", email = "u2@example.com", name = "User Two")
    private val userMap = mapOf("u1" to user1, "u2" to user2)

    private val tour = Tour(id = "t1", title = "Bandarban Adventure", inviteCode = "BND01", createdBy = "u1")
    private val expenses = listOf(
        Expense(
            id = "e1",
            tourId = "t1",
            title = "BBQ Dinner",
            amount = 4000L,
            category = ExpenseCategory.FOOD,
            splitType = SplitType.EQUAL,
            payments = listOf(Payment("u1", 4000L)),
            allocations = listOf(Allocation("u1", 2000L), Allocation("u2", 2000L)),
            createdBy = "u1"
        )
    )
    private val settlements = listOf(
        Settlement(
            id = "s1",
            tourId = "t1",
            fromUserId = "u2",
            toUserId = "u1",
            amount = 2000L,
            paymentMethod = PaymentMethod.BKASH,
            createdBy = "u2"
        )
    )
    private val balanceSummary = TourBalanceSummary(
        tourId = "t1",
        totalCost = 4000L,
        totalSharedCost = 4000L,
        totalPersonalCost = 0L,
        balances = mapOf(
            "u1" to UserBalance("u1", totalPaid = 4000L, sharedPaid = 4000L, sharedResponsible = 2000L, netBalance = 0L),
            "u2" to UserBalance("u2", totalPaid = 0L, sharedPaid = 0L, sharedResponsible = 2000L, netBalance = 0L)
        ),
        suggestedTransfers = emptyList()
    )

    @Test
    fun `generateExpensesCsv should produce valid CSV content`() {
        val bytes = csvExportService.generateExpensesCsv(expenses, userMap)
        val csvString = String(bytes, StandardCharsets.UTF_8)

        assertTrue(csvString.contains("Date,Title,Category,Amount (Major)"))
        assertTrue(csvString.contains("BBQ Dinner"))
        assertTrue(csvString.contains("FOOD"))
        assertTrue(csvString.contains("User One"))
    }

    @Test
    fun `generateTourPdf should produce non-empty PDF document bytes`() {
        val bytes = pdfExportService.generateTourPdf(tour, expenses, balanceSummary, settlements, userMap)

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())
        // PDF header magic bytes "%PDF-"
        val header = String(bytes.copyOfRange(0, 5), StandardCharsets.US_ASCII)
        assertEquals("%PDF-", header)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual)
    }
}
