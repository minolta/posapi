package me.pixka.pos.order.api

import java.time.LocalDate

data class DailyOrderReport(
    val date: LocalDate,
    val orderCount: Int,
    val paidOrderCount: Int,
    val cancelledOrderCount: Int,
    val grossSales: Double,
)

data class ReportLineItem(
    val foodCode: String,
    val foodName: String,
    val quantity: Int,
    val total: Double,
)

data class OrderReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val orderCount: Int,
    val paidOrderCount: Int,
    val cancelledOrderCount: Int,
    val grossSales: Double,
    val daily: List<DailyOrderReport>,
    val items: List<ReportLineItem>,
)
