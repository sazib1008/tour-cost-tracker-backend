package com.example.tripzy_backend.balance.service

import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.Payment
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
    fun `exact zero-sum when everyone paid their own share produces 0 transfers`() {
        val expenses = listOf(
            Expense(
                tourId = "t1",
                title = "Meal 1",
                amount = 1000L,
                payments = listOf(Payment("u1", 1000L)),
                allocations = listOf(Allocation("u1", 1000L)),
                createdBy = "u1"
            ),
            Expense(
                tourId = "t1",
                title = "Meal 2",
                amount = 2000L,
                payments = listOf(Payment("u2", 2000L)),
                allocations = listOf(Allocation("u2", 2000L)),
                createdBy = "u2"
            )
        )

        val summary = calculator.calculate("t1", setOf("u1", "u2"), expenses)

        assertEquals(0L, summary.balances["u1"]?.netBalance)
        assertEquals(0L, summary.balances["u2"]?.netBalance)
        assertTrue(summary.suggestedTransfers.isEmpty())
    }

    @Test
    fun `single payer and multiple debtors simplifies into direct transfers`() {
        // A paid 3000 for A, B, C (1000 each)
        val expense = Expense(
            tourId = "t1",
            title = "Dinner",
            amount = 3000L,
            payments = listOf(Payment("A", 3000L)),
            allocations = listOf(
                Allocation("A", 1000L),
                Allocation("B", 1000L),
                Allocation("C", 1000L)
            ),
            createdBy = "A"
        )

        val summary = calculator.calculate("t1", setOf("A", "B", "C"), listOf(expense))

        assertEquals(2000L, summary.balances["A"]?.netBalance) // will receive 2000
        assertEquals(-1000L, summary.balances["B"]?.netBalance) // owes 1000
        assertEquals(-1000L, summary.balances["C"]?.netBalance) // owes 1000

        assertEquals(2, summary.suggestedTransfers.size)
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "B" && it.toUserId == "A" && it.amount == 1000L })
        assertTrue(summary.suggestedTransfers.any { it.fromUserId == "C" && it.toUserId == "A" && it.amount == 1000L })
    }

    @Test
    fun `cyclic debts cancel out to zero transfers`() {
        // A pays 1000 for B
        val exp1 = Expense(
            tourId = "t1",
            title = "A for B",
            amount = 1000L,
            payments = listOf(Payment("A", 1000L)),
            allocations = listOf(Allocation("B", 1000L)),
            createdBy = "A"
        )
        // B pays 1000 for C
        val exp2 = Expense(
            tourId = "t1",
            title = "B for C",
            amount = 1000L,
            payments = listOf(Payment("B", 1000L)),
            allocations = listOf(Allocation("C", 1000L)),
            createdBy = "B"
        )
        // C pays 1000 for A
        val exp3 = Expense(
            tourId = "t1",
            title = "C for A",
            amount = 1000L,
            payments = listOf(Payment("C", 1000L)),
            allocations = listOf(Allocation("A", 1000L)),
            createdBy = "C"
        )

        val summary = calculator.calculate("t1", setOf("A", "B", "C"), listOf(exp1, exp2, exp3))

        assertEquals(0L, summary.balances["A"]?.netBalance)
        assertEquals(0L, summary.balances["B"]?.netBalance)
        assertEquals(0L, summary.balances["C"]?.netBalance)
        assertTrue(summary.suggestedTransfers.isEmpty())
    }

    @Test
    fun `personal expense does not affect group settlement`() {
        // A has personal expense of 5000
        val personalExp = Expense(
            tourId = "t1",
            title = "Personal Watch",
            amount = 5000L,
            isPersonal = true,
            payments = listOf(Payment("A", 5000L)),
            allocations = listOf(Allocation("A", 5000L)),
            createdBy = "A"
        )

        // B pays 2000 shared for A and B (1000 each)
        val sharedExp = Expense(
            tourId = "t1",
            title = "Lunch",
            amount = 2000L,
            isPersonal = false,
            payments = listOf(Payment("B", 2000L)),
            allocations = listOf(
                Allocation("A", 1000L),
                Allocation("B", 1000L)
            ),
            createdBy = "B"
        )

        val summary = calculator.calculate("t1", setOf("A", "B"), listOf(personalExp, sharedExp))

        assertEquals(7000L, summary.totalCost)
        assertEquals(2000L, summary.totalSharedCost)
        assertEquals(5000L, summary.totalPersonalCost)

        val balanceA = summary.balances["A"]!!
        assertEquals(5000L, balanceA.personalSpend)
        assertEquals(0L, balanceA.sharedPaid)
        assertEquals(1000L, balanceA.sharedResponsible)
        assertEquals(-1000L, balanceA.netBalance) // A owes 1000 to B for lunch, personal expense ignored

        val balanceB = summary.balances["B"]!!
        assertEquals(0L, balanceB.personalSpend)
        assertEquals(2000L, balanceB.sharedPaid)
        assertEquals(1000L, balanceB.sharedResponsible)
        assertEquals(1000L, balanceB.netBalance)

        assertEquals(1, summary.suggestedTransfers.size)
        assertEquals("A", summary.suggestedTransfers[0].fromUserId)
        assertEquals("B", summary.suggestedTransfers[0].toUserId)
        assertEquals(1000L, summary.suggestedTransfers[0].amount)
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
