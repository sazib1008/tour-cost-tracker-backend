package com.example.tripzy_backend.export.service

import com.example.tripzy_backend.balance.model.TourBalanceSummary
import com.example.tripzy_backend.common.domain.Money
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.settlement.model.Settlement
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.user.model.User
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class PdfExportService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

    fun generateTourPdf(
        tour: Tour,
        expenses: List<Expense>,
        balanceSummary: TourBalanceSummary,
        settlements: List<Settlement>,
        userMap: Map<String, User>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, out)
        document.open()

        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, Color(33, 33, 33))
        val subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11f, Color(100, 100, 100))
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, Color(41, 98, 255))
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)
        val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY)
        val boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK)

        // Title Header
        val titlePara = Paragraph("Tripzy — Tour Cost & Settlement Report", titleFont)
        titlePara.alignment = Element.ALIGN_CENTER
        document.add(titlePara)

        val tourNamePara = Paragraph("Tour: ${tour.title} (Status: ${tour.status})", subTitleFont)
        tourNamePara.alignment = Element.ALIGN_CENTER
        tourNamePara.spacingAfter = 15f
        document.add(tourNamePara)

        // Summary KPI Table
        val kpiTable = PdfPTable(3)
        kpiTable.widthPercentage = 100f
        kpiTable.setSpacingAfter(15f)

        fun addKpiCell(label: String, value: String) {
            val cell = PdfPCell()
            cell.backgroundColor = Color(245, 247, 250)
            cell.setPadding(8f)
            cell.addElement(Paragraph(label, subTitleFont))
            cell.addElement(Paragraph(value, boldCellFont))
            kpiTable.addCell(cell)
        }

        addKpiCell("Total Tour Cost", Money.ofPaisa(balanceSummary.totalCost, tour.baseCurrency).formatted())
        addKpiCell("Total Shared Cost", Money.ofPaisa(balanceSummary.totalSharedCost, tour.baseCurrency).formatted())
        addKpiCell("Total Personal Cost", Money.ofPaisa(balanceSummary.totalPersonalCost, tour.baseCurrency).formatted())
        document.add(kpiTable)

        // Section: Member Balances
        document.add(Paragraph("Member Balances & Settlements", sectionFont))
        val balanceTable = PdfPTable(floatArrayOf(3f, 2f, 2f, 2f, 2f))
        balanceTable.widthPercentage = 100f
        balanceTable.setSpacingBefore(8f)
        balanceTable.setSpacingAfter(15f)

        val balanceHeaders = listOf("Member", "Total Paid", "Shared Share", "Personal", "Net Balance")
        balanceHeaders.forEach { h ->
            val cell = PdfPCell(Phrase(h, headerFont))
            cell.backgroundColor = Color(41, 98, 255)
            cell.setPadding(5f)
            balanceTable.addCell(cell)
        }

        balanceSummary.balances.values.forEach { b ->
            val u = userMap[b.userId]
            val netStr = Money.ofPaisa(b.netBalance, tour.baseCurrency).formatted()
            val netFormatted = if (b.netBalance > 0) "+$netStr (Receives)" else if (b.netBalance < 0) "$netStr (Owes)" else "Settled (৳0)"

            balanceTable.addCell(PdfPCell(Phrase(u?.name ?: b.userId, cellFont)))
            balanceTable.addCell(PdfPCell(Phrase(Money.ofPaisa(b.totalPaid, tour.baseCurrency).formatted(), cellFont)))
            balanceTable.addCell(PdfPCell(Phrase(Money.ofPaisa(b.sharedResponsible, tour.baseCurrency).formatted(), cellFont)))
            balanceTable.addCell(PdfPCell(Phrase(Money.ofPaisa(b.personalSpend, tour.baseCurrency).formatted(), cellFont)))
            balanceTable.addCell(PdfPCell(Phrase(netFormatted, boldCellFont)))
        }
        document.add(balanceTable)

        // Section: Suggested Transfers
        if (balanceSummary.suggestedTransfers.isNotEmpty()) {
            document.add(Paragraph("Suggested Debt Transfers (Who Pays Whom)", sectionFont))
            val transferTable = PdfPTable(floatArrayOf(3f, 3f, 2f))
            transferTable.widthPercentage = 100f
            transferTable.setSpacingBefore(8f)
            transferTable.setSpacingAfter(15f)

            listOf("Debtor (From)", "Creditor (To)", "Amount").forEach { h ->
                val cell = PdfPCell(Phrase(h, headerFont))
                cell.backgroundColor = Color(55, 71, 79)
                cell.setPadding(5f)
                transferTable.addCell(cell)
            }

            balanceSummary.suggestedTransfers.forEach { t ->
                val fromName = userMap[t.fromUserId]?.name ?: t.fromUserId
                val toName = userMap[t.toUserId]?.name ?: t.toUserId
                transferTable.addCell(PdfPCell(Phrase(fromName, cellFont)))
                transferTable.addCell(PdfPCell(Phrase(toName, cellFont)))
                transferTable.addCell(PdfPCell(Phrase(Money.ofPaisa(t.amount, tour.baseCurrency).formatted(), boldCellFont)))
            }
            document.add(transferTable)
        }

        // Section: Expenses Ledger
        document.add(Paragraph("Expense Ledger (${expenses.size} expenses)", sectionFont))
        val expenseTable = PdfPTable(floatArrayOf(2f, 3.5f, 1.8f, 2.5f, 2f))
        expenseTable.widthPercentage = 100f
        expenseTable.setSpacingBefore(8f)
        expenseTable.setSpacingAfter(15f)

        listOf("Date", "Title", "Category", "Paid By", "Amount").forEach { h ->
            val cell = PdfPCell(Phrase(h, headerFont))
            cell.backgroundColor = Color(38, 50, 56)
            cell.setPadding(5f)
            expenseTable.addCell(cell)
        }

        expenses.forEach { exp ->
            val dateStr = dateFormatter.format(exp.expenseDate)
            val paidBy = exp.payments.joinToString { userMap[it.userId]?.name ?: it.userId }
            val typeBadge = if (exp.isPersonal) " [Personal]" else ""

            expenseTable.addCell(PdfPCell(Phrase(dateStr, cellFont)))
            expenseTable.addCell(PdfPCell(Phrase("${exp.title}$typeBadge", cellFont)))
            expenseTable.addCell(PdfPCell(Phrase(exp.category.name, cellFont)))
            expenseTable.addCell(PdfPCell(Phrase(paidBy, cellFont)))
            expenseTable.addCell(PdfPCell(Phrase(Money.ofPaisa(exp.amount, exp.currency).formatted(), boldCellFont)))
        }
        document.add(expenseTable)

        // Section: Settlement History
        if (settlements.isNotEmpty()) {
            document.add(Paragraph("Settlement Payment History", sectionFont))
            val settlementTable = PdfPTable(floatArrayOf(2f, 2.5f, 2.5f, 1.8f, 2f))
            settlementTable.widthPercentage = 100f
            settlementTable.setSpacingBefore(8f)
            settlementTable.setSpacingAfter(15f)

            listOf("Date", "Payer", "Payee", "Method", "Amount").forEach { h ->
                val cell = PdfPCell(Phrase(h, headerFont))
                cell.backgroundColor = Color(46, 125, 50)
                cell.setPadding(5f)
                settlementTable.addCell(cell)
            }

            settlements.forEach { s ->
                val dateStr = dateFormatter.format(s.settledAt)
                val fromName = userMap[s.fromUserId]?.name ?: s.fromUserId
                val toName = userMap[s.toUserId]?.name ?: s.toUserId

                settlementTable.addCell(PdfPCell(Phrase(dateStr, cellFont)))
                settlementTable.addCell(PdfPCell(Phrase(fromName, cellFont)))
                settlementTable.addCell(PdfPCell(Phrase(toName, cellFont)))
                settlementTable.addCell(PdfPCell(Phrase(s.paymentMethod.name, cellFont)))
                settlementTable.addCell(PdfPCell(Phrase(Money.ofPaisa(s.amount, tour.baseCurrency).formatted(), boldCellFont)))
            }
            document.add(settlementTable)
        }

        document.close()
        return out.toByteArray()
    }
}
