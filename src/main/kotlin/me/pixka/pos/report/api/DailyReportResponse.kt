package me.pixka.pos.report.api

import java.time.LocalDate
import java.time.LocalDateTime

/** Response for `GET /api/reports/daily` (matches POS front-end daily report screen). */
data class DailyReportResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val orderCount: Int,
    val paidOrderCount: Int,
    /** Paid orders settled with QR/mobile wallet (`paid_by_qr_scan`). */
    val paidByQrScanOrderCount: Int,
    /** Paid orders settled by credit card (`paid_by_credit`). */
    val paidByCreditOrderCount: Int,
    val totalSales: Double,
    val totalCashReceived: Double,
    val totalChange: Double,
    val rows: List<DailyReportRow>,
    /** Aggregated sales by food for paid orders in the range (non-cancelled lines). */
    val foods: List<DailyReportFoodRow>,
)

data class DailyReportRow(
    val orderId: Long,
    val orderNo: String,
    val paidAt: LocalDateTime?,
    val totalDue: Double,
    val paidPrice: Double?,
    val change: Double?,
    /** Settled via scanned QR / mobile wallet. */
    val paidByQrScan: Boolean,
    /** Settled by credit card. */
    val paidByCredit: Boolean,
)
