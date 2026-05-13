package me.pixka.pos.report.api

/** Paid line rollup for `GET /api/reports/daily` (qty × unitPrice, non-cancelled lines). */
data class DailyReportFoodRow(
    val foodCode: String,
    val foodName: String,
    val quantity: Int,
    val total: Double,
)
