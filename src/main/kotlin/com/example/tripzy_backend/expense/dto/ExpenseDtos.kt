package com.example.tripzy_backend.expense.dto

import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.ExpenseCategory
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.expense.model.SplitType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class PaymentDto(
    @field:NotBlank(message = "Payment userId is required")
    val userId: String,

    @field:Min(value = 1, message = "Payment amount must be greater than zero")
    val amount: Long // in Paisa
)

data class AllocationDto(
    @field:NotBlank(message = "Allocation userId is required")
    val userId: String,

    @field:Min(value = 0, message = "Allocation amount cannot be negative")
    val amount: Long // in Paisa
)

data class CreateExpenseRequest(
    @field:NotBlank(message = "Expense title is required")
    @field:Size(min = 2, max = 100, message = "Expense title must be between 2 and 100 characters")
    val title: String,

    val description: String? = null,

    @field:Min(value = 1, message = "Expense amount must be greater than zero")
    val amount: Long, // in Paisa

    val currency: String = "BDT",

    val category: ExpenseCategory = ExpenseCategory.OTHER,

    val splitType: SplitType = SplitType.EQUAL,

    val isPersonal: Boolean = false,

    val expenseDate: Instant? = null,

    val receiptUrl: String? = null,

    val payments: List<PaymentDto>? = null,

    val allocations: List<AllocationDto>? = null,

    val participants: List<String>? = null,

    val percentages: Map<String, Double>? = null,

    val shares: Map<String, Int>? = null
)

data class UpdateExpenseRequest(
    @field:NotBlank(message = "Expense title is required")
    @field:Size(min = 2, max = 100, message = "Expense title must be between 2 and 100 characters")
    val title: String,

    val description: String? = null,

    @field:Min(value = 1, message = "Expense amount must be greater than zero")
    val amount: Long,

    val currency: String = "BDT",

    val category: ExpenseCategory = ExpenseCategory.OTHER,

    val splitType: SplitType = SplitType.EQUAL,

    val isPersonal: Boolean = false,

    val expenseDate: Instant? = null,

    val receiptUrl: String? = null,

    val payments: List<PaymentDto>? = null,

    val allocations: List<AllocationDto>? = null,

    val participants: List<String>? = null,

    val percentages: Map<String, Double>? = null,

    val shares: Map<String, Int>? = null
)

data class ExpenseDto(
    val id: String,
    val tourId: String,
    val title: String,
    val description: String?,
    val amount: Long, // in Paisa
    val currency: String,
    val baseCurrencyAmount: Long,
    val exchangeRate: Double,
    val category: ExpenseCategory,
    val splitType: SplitType,
    val isPersonal: Boolean,
    val expenseDate: Instant,
    val payments: List<PaymentDto>,
    val allocations: List<AllocationDto>,
    val createdBy: String,
    val receiptUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun fromEntity(expense: Expense): ExpenseDto = ExpenseDto(
            id = expense.id ?: "",
            tourId = expense.tourId,
            title = expense.title,
            description = expense.description,
            amount = expense.amount,
            currency = expense.currency,
            baseCurrencyAmount = expense.baseCurrencyAmount,
            exchangeRate = expense.exchangeRate,
            category = expense.category,
            splitType = expense.splitType,
            isPersonal = expense.isPersonal,
            expenseDate = expense.expenseDate,
            payments = expense.payments.map { PaymentDto(it.userId, it.amount) },
            allocations = expense.allocations.map { AllocationDto(it.userId, it.amount) },
            createdBy = expense.createdBy,
            receiptUrl = expense.receiptUrl,
            createdAt = expense.createdAt,
            updatedAt = expense.updatedAt
        )
    }
}
