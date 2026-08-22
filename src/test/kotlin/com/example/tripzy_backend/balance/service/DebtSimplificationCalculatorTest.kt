package com.example.tripzy_backend.balance.service

import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.settlement.model.Settlement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DebtSimplificationCalculatorTest {

    private lateinit var calculator: DebtSimplificationCalculator

    @BeforeEach
    fun setUp() {
        calculator = DebtSimplificationCalculator()
    }

    @Test
    fun `individual expenses for a member totaling 735 are correctly aggregated without missing entries`() {
        // User A has 3 individual expenses: 500, 205, and 30 (totaling 735)
        // Expense 3 tests fallback when payments/allocations list is empty
        val exp1 = Expense(
            tourId = "t1",
            title = "Item 1",
            amount = 500L,
            isPersonal = true,
            payments = listOf(Payment("u1", 500L)),
            allocations = listOf(Allocation("u1", 500L)),
            createdBy = "u1"
        )
        val exp2 = Expense(
            tourId = "t1",
            title = "Item 2",
            amount = 205L,
            isPersonal = true,
            payments = listOf(Payment("u1", 205L)),
            allocations = listOf(Allocation("u1", 205L)),
            createdBy = "u1"
        )
        val exp3 = Expense(
            tourId = "t1",
            title = "Item 3",
            amount = 30L,
            isPersonal = true,
            payments = emptyList(), // implicit payer: createdBy
            allocations = emptyList(), // implicit recipient: createdBy
            createdBy = "u1"
        )

        val summary = calculator.calculate("t1", setOf("u1"), listOf(exp1, exp2, exp3))

        val u1Balance = summary.balances["u1"]!!
        assertEquals(735L, u1Balance.totalPaid, "Total paid must aggregate all entries (500 + 205 + 30 = 735)")
        assertEquals(735L, u1Balance.personalSpend, "Personal spend must equal 735")
        assertEquals(0L, u1Balance.sharedResponsible, "Shared responsible must be 0")
        assertEquals(0L, u1Balance.netBalance, "Net balance should be 0 since u1 paid for own personal items")
        assertEquals(735L, summary.totalPersonalCost)
        assertEquals(735L, summary.totalCost)
        assertTrue(summary.suggestedTransfers.isEmpty())
    }

    @Test
    fun `shared cost distribution divides total shared cost equally among active members`() {
        // Total shared cost = 3000, 3 active members (u1, u2, u3)
        // u1 pays the full 3000
        val expense = Expense(
            tourId = "t1",
            title = "Group Villa Rental",
            amount = 3000L,
            isPersonal = false,
            payments = listOf(Payment("u1", 3000L)),
            createdBy = "u1"
        )

        val summary = calculator.calculate("t1", setOf("u1", "u2", "u3"), listOf(expense))

        assertEquals(3000L, summary.totalCost)
        assertEquals(3000L, summary.totalSharedCost)
        assertEquals(0L, summary.totalPersonalCost)

        // Each member's share = 3000 / 3 = 1000
        val b1 = summary.balances["u1"]!!
        val b2 = summary.balances["u2"]!!
        val b3 = summary.balances["u3"]!!

        assertEquals(1000L, b1.sharedResponsible)
        assertEquals(1000L, b2.sharedResponsible)
        assertEquals(1000L, b3.sharedResponsible)

        // Net Balance = totalPaid - (sharedShare + personalShare)
        // u1: 3000 - 1000 = +2000 (Creditor)
        // u2: 0 - 1000 = -1000 (Debtor)
        // u3: 0 - 1000 = -1000 (Debtor)
        assertEquals(2000L, b1.netBalance)
        assertEquals(-1000L, b2.netBalance)
        assertEquals(-1000L, b3.netBalance)

        // Direction: Debtors (u2, u3) pay Creditor (u1)
        assertEquals(2, summary.suggestedTransfers.size)
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "u2" && it.toUserId == "u1" && it.amount == 1000L })
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "u3" && it.toUserId == "u1" && it.amount == 1000L })
    }

    @Test
    fun `shared cost equal split handles odd remainders deterministically without loss`() {
        // Shared cost = 1000, 3 active members -> 334, 333, 333 (sum = 1000)
        val expense = Expense(
            tourId = "t1",
            title = "Snacks",
            amount = 1000L,
            isPersonal = false,
            payments = listOf(Payment("A", 1000L)),
            createdBy = "A"
        )

        val summary = calculator.calculate("t1", setOf("A", "B", "C"), listOf(expense))

        val bA = summary.balances["A"]!!
        val bB = summary.balances["B"]!!
        val bC = summary.balances["C"]!!

        val totalShare = bA.sharedResponsible + bB.sharedResponsible + bC.sharedResponsible
        assertEquals(1000L, totalShare, "Sum of individual shares must equal total shared cost exactly")

        // Member A (sorted first) gets 334, B and C get 333
        assertEquals(334L, bA.sharedResponsible)
        assertEquals(333L, bB.sharedResponsible)
        assertEquals(333L, bC.sharedResponsible)

        // Net balance:
        // A: 1000 - 334 = +666
        // B: 0 - 333 = -333
        // C: 0 - 333 = -333
        assertEquals(666L, bA.netBalance)
        assertEquals(-333L, bB.netBalance)
        assertEquals(-333L, bC.netBalance)

        assertEquals(2, summary.suggestedTransfers.size)
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "B" && it.toUserId == "A" && it.amount == 333L })
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "C" && it.toUserId == "A" && it.amount == 333L })
    }

    @Test
    fun `debtor to creditor directional transfers correctly map debtor to creditor`() {
        // A paid 2000 (shared), B paid 0 (shared) for A and B
        val expense = Expense(
            tourId = "t1",
            title = "Dinner",
            amount = 2000L,
            isPersonal = false,
            payments = listOf(Payment("A", 2000L)),
            createdBy = "A"
        )

        val summary = calculator.calculate("t1", setOf("A", "B"), listOf(expense))

        val bA = summary.balances["A"]!!
        val bB = summary.balances["B"]!!

        assertTrue(bA.netBalance > 0, "A is Creditor (netBalance > 0)")
        assertTrue(bB.netBalance < 0, "B is Debtor (netBalance < 0)")
        assertEquals(1000L, bA.netBalance)
        assertEquals(-1000L, bB.netBalance)

        assertEquals(1, summary.suggestedTransfers.size)
        val transfer = summary.suggestedTransfers.first()
        assertEquals("B", transfer.fromUserId, "Debtor B must be fromUserId (paying)")
        assertEquals("A", transfer.toUserId, "Creditor A must be toUserId (receiving)")
        assertEquals(1000L, transfer.amount)
    }

    @Test
    fun `mixed shared and personal expenses calculate exact net balance and settlement transfers`() {
        // Member A: Personal watch for 5000 (paid by A)
        val exp1 = Expense(
            tourId = "t1",
            title = "Watch",
            amount = 5000L,
            isPersonal = true,
            payments = listOf(Payment("A", 5000L)),
            allocations = listOf(Allocation("A", 5000L)),
            createdBy = "A"
        )
        // Shared Lunch of 2000: Paid by B for A and B (equal split 1000 each)
        val exp2 = Expense(
            tourId = "t1",
            title = "Lunch",
            amount = 2000L,
            isPersonal = false,
            payments = listOf(Payment("B", 2000L)),
            createdBy = "B"
        )

        val summary = calculator.calculate("t1", setOf("A", "B"), listOf(exp1, exp2))

        assertEquals(7000L, summary.totalCost)
        assertEquals(2000L, summary.totalSharedCost)
        assertEquals(5000L, summary.totalPersonalCost)

        val balA = summary.balances["A"]!!
        val balB = summary.balances["B"]!!

        // A: totalPaid = 5000, personalSpend = 5000, sharedResponsible = 1000
        // netBalance = 5000 - (1000 + 5000) = -1000 (Debtor owes 1000)
        assertEquals(5000L, balA.totalPaid)
        assertEquals(5000L, balA.personalSpend)
        assertEquals(1000L, balA.sharedResponsible)
        assertEquals(-1000L, balA.netBalance)

        // B: totalPaid = 2000, personalSpend = 0, sharedResponsible = 1000
        // netBalance = 2000 - (1000 + 0) = +1000 (Creditor receives 1000)
        assertEquals(2000L, balB.totalPaid)
        assertEquals(0L, balB.personalSpend)
        assertEquals(1000L, balB.sharedResponsible)
        assertEquals(1000L, balB.netBalance)

        assertEquals(1, summary.suggestedTransfers.size)
        assertEquals("A", summary.suggestedTransfers[0].fromUserId)
        assertEquals("B", summary.suggestedTransfers[0].toUserId)
        assertEquals(1000L, summary.suggestedTransfers[0].amount)
    }

    @Test
    fun `recorded settlements reduce debtor debt and creditor credit to zero`() {
        // Shared expense: B pays 2000 for A and B
        val expense = Expense(
            tourId = "t1",
            title = "Hotel",
            amount = 2000L,
            isPersonal = false,
            payments = listOf(Payment("B", 2000L)),
            createdBy = "B"
        )

        // A settles 1000 to B
        val settlement = Settlement(
            tourId = "t1",
            fromUserId = "A",
            toUserId = "B",
            amount = 1000L,
            createdBy = "A"
        )

        val summary = calculator.calculate("t1", setOf("A", "B"), listOf(expense), listOf(settlement))

        val balA = summary.balances["A"]!!
        val balB = summary.balances["B"]!!

        assertEquals(0L, balA.netBalance)
        assertEquals(0L, balB.netBalance)
        assertTrue(balA.isSettled())
        assertTrue(balB.isSettled())
        assertTrue(summary.suggestedTransfers.isEmpty())
    }

    @Test
    fun `complex debt network with 5 members simplifies with minimal transfers`() {
        // Net balances:
        // User A: +5000
        // User B: +3000
        // User C: -4000
        // User D: -3000
        // User E: -1000
        // Sum of positives = +8000, Sum of negatives = -8000
        val balances = listOf(
            UserBalance("A", netBalance = 5000L),
            UserBalance("B", netBalance = 3000L),
            UserBalance("C", netBalance = -4000L),
            UserBalance("D", netBalance = -3000L),
            UserBalance("E", netBalance = -1000L)
        )

        val transfers = calculator.simplifyDebts(balances)

        // Verifies all transfers satisfy debts completely
        val transferDebits = mutableMapOf<String, Long>().withDefault { 0L }
        val transferCredits = mutableMapOf<String, Long>().withDefault { 0L }

        transfers.forEach { t ->
            transferDebits[t.fromUserId] = transferDebits.getValue(t.fromUserId) + t.amount
            transferCredits[t.toUserId] = transferCredits.getValue(t.toUserId) + t.amount
        }

        assertEquals(5000L, transferCredits["A"])
        assertEquals(3000L, transferCredits["B"])
        assertEquals(4000L, transferDebits["C"])
        assertEquals(3000L, transferDebits["D"])
        assertEquals(1000L, transferDebits["E"])

        // N members should take at most N-1 transactions
        assertTrue(transfers.size <= 4)
    }
}

