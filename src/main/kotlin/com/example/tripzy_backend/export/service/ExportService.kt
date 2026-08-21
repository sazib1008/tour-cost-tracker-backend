package com.example.tripzy_backend.export.service

import com.example.tripzy_backend.balance.service.DebtSimplificationCalculator
import com.example.tripzy_backend.expense.repository.ExpenseRepository
import com.example.tripzy_backend.settlement.repository.SettlementRepository
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.service.TourService
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service

data class ExportResult(
    val fileName: String,
    val contentType: String,
    val data: ByteArray
)

@Service
class ExportService(
    private val tourService: TourService,
    private val tourMemberRepository: TourMemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val userRepository: UserRepository,
    private val calculator: DebtSimplificationCalculator,
    private val csvExportService: CsvExportService,
    private val pdfExportService: PdfExportService
) {

    fun exportTour(userId: String, tourId: String, format: String): ExportResult {
        tourService.assertUserIsMember(userId, tourId)
        val tour = tourService.findTourById(tourId)

        val members = tourMemberRepository.findByTourId(tourId)
        val memberUserIds = members.map { it.userId }.toSet()
        val expenses = expenseRepository.findByTourIdOrderByExpenseDateDesc(tourId)
        val settlements = settlementRepository.findByTourIdOrderBySettledAtDesc(tourId)

        val allUserIds = (memberUserIds + expenses.flatMap { it.payments.map { p -> p.userId } + it.allocations.map { a -> a.userId } } + settlements.map { it.fromUserId } + settlements.map { it.toUserId }).distinct()
        val userMap = userRepository.findAllById(allUserIds).associateBy { it.id ?: "" }

        val safeTitle = tour.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        return when (format.lowercase()) {
            "csv" -> {
                val csvData = csvExportService.generateExpensesCsv(expenses, userMap)
                ExportResult(
                    fileName = "${safeTitle}_expenses.csv",
                    contentType = "text/csv",
                    data = csvData
                )
            }
            "pdf" -> {
                val balanceSummary = calculator.calculate(tourId, memberUserIds, expenses, settlements)
                val pdfData = pdfExportService.generateTourPdf(tour, expenses, balanceSummary, settlements, userMap)
                ExportResult(
                    fileName = "${safeTitle}_report.pdf",
                    contentType = "application/pdf",
                    data = pdfData
                )
            }
            else -> throw IllegalArgumentException("Unsupported export format: '$format'. Supported formats: 'pdf', 'csv'")
        }
    }
}
