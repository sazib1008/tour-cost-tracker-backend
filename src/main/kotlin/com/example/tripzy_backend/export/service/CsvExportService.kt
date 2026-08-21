package com.example.tripzy_backend.export.service

import com.example.tripzy_backend.common.domain.Money
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.user.model.User
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class CsvExportService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

    fun generateExpensesCsv(
        expenses: List<Expense>,
        userMap: Map<String, User>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        PrintWriter(out, true, StandardCharsets.UTF_8).use { writer ->
            // Header
            writer.println("Date,Title,Category,Amount (Major),Amount (Paisa),Currency,Is Personal,Paid By,Split Type,Allocations")

            for (expense in expenses) {
                val dateStr = dateFormatter.format(expense.expenseDate)
                val title = escapeCsv(expense.title)
                val category = expense.category.name
                val amountMajor = Money.ofPaisa(expense.amount, expense.currency).formatted()
                val amountPaisa = expense.amount.toString()
                val currency = expense.currency
                val isPersonal = if (expense.isPersonal) "Yes" else "No"
                val splitType = expense.splitType.name

                val paidByStr = expense.payments.joinToString("; ") { p ->
                    val name = userMap[p.userId]?.name ?: p.userId
                    val amt = Money.ofPaisa(p.amount, expense.currency).formatted()
                    "$name ($amt)"
                }

                val allocStr = expense.allocations.joinToString("; ") { a ->
                    val name = userMap[a.userId]?.name ?: a.userId
                    val amt = Money.ofPaisa(a.amount, expense.currency).formatted()
                    "$name ($amt)"
                }

                writer.println(
                    "$dateStr,$title,$category,$amountMajor,$amountPaisa,$currency,$isPersonal,\"$paidByStr\",$splitType,\"$allocStr\""
                )
            }
        }
        return out.toByteArray()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
