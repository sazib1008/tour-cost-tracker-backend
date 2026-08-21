package com.example.tripzy_backend.expense.model

import com.example.tripzy_backend.common.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "expenses")
data class Expense(
    @Id
    var id: String? = null,

    @Indexed
    var tourId: String,

    var title: String,

    var description: String? = null,

    var amount: Long, // Stored in Paisa (1 BDT = 100 Paisa)

    var currency: String = "BDT",

    var baseCurrencyAmount: Long = amount,

    var exchangeRate: Double = 1.0,

    var category: ExpenseCategory = ExpenseCategory.OTHER,

    var splitType: SplitType = SplitType.EQUAL,

    var isPersonal: Boolean = false,

    var expenseDate: Instant = Instant.now(),

    var payments: List<Payment> = emptyList(),

    var allocations: List<Allocation> = emptyList(),

    @Indexed
    var createdBy: String,

    var receiptUrl: String? = null
) : BaseEntity() {

    fun validateInvariants() {
        val paymentsSum = payments.sumOf { it.amount }
        val allocationsSum = allocations.sumOf { it.amount }

        require(paymentsSum == amount) {
            "Sum of payments ($paymentsSum paisa) does not equal expense amount ($amount paisa)"
        }

        require(allocationsSum == amount) {
            "Sum of allocations ($allocationsSum paisa) does not equal expense amount ($amount paisa)"
        }
    }
}
