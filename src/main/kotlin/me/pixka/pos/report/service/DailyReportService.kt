package me.pixka.pos.report.service

import me.pixka.pos.order.model.OrderLine
import me.pixka.pos.order.model.OrderLineStatus
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.report.api.DailyReportFoodRow
import me.pixka.pos.report.api.DailyReportResponse
import me.pixka.pos.report.api.DailyReportRow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DailyReportService(
    private val orderRepository: OrderRepository,
) {
    /**
     * Cash-oriented report: paid orders whose settlement day (`paidAt` or `orderDate`) falls in `[start, end]`.
     * `orderCount` counts orders that overlap the range by created day or (if paid) settlement day—mirrors the Angular fallback.
     */
    @Transactional(readOnly = true)
    fun buildDailyCashReport(start: LocalDate, end: LocalDate): DailyReportResponse {
        require(!end.isBefore(start)) { "endDate must be on or after startDate" }
        val orders = orderRepository.findAll()

        val touchIds = LinkedHashSet<Long>()
        val rows = mutableListOf<DailyReportRow>()
        /** foodId → rollup */
        val foodRollup = mutableMapOf<Long, FoodAgg>()
        var paidOrderCount = 0
        var paidByQrScanOrderCount = 0
        var paidByCreditOrderCount = 0
        var totalSales = 0.0
        var totalCashReceived = 0.0
        var totalChange = 0.0

        for (order in orders) {
            val id = order.id ?: continue
            val created = orderCreatedDate(order)
            val settle = paidSettlementDate(order)
            if (created != null && inInclusiveRange(created, start, end)) {
                touchIds.add(id)
            }
            if (settle != null && inInclusiveRange(settle, start, end)) {
                touchIds.add(id)
            }
            if (!order.paid || settle == null || !inInclusiveRange(settle, start, end)) {
                continue
            }
            paidOrderCount += 1
            if (order.paidByQrScan) {
                paidByQrScanOrderCount += 1
            }
            if (order.paidByCredit) {
                paidByCreditOrderCount += 1
            }
            val due = roundMoney(payableTotal(order))
            totalSales += due
            val pp = roundMoney(order.paidPrice)
            val ch = roundMoney(order.change)
            if (!order.paidByQrScan && !order.paidByCredit) {
                totalCashReceived += pp
                totalChange += ch
            }
            rows.add(
                DailyReportRow(
                    orderId = id,
                    orderNo = order.orderNo,
                    paidAt = order.paidAt,
                    totalDue = due,
                    paidPrice = pp,
                    change = ch,
                    paidByQrScan = order.paidByQrScan,
                    paidByCredit = order.paidByCredit,
                ),
            )
            rollupFoodLines(order, foodRollup)
        }
        rows.sortBy { it.orderId }

        val foods = foodRollup.values
            .map {
                DailyReportFoodRow(
                    foodCode = it.code,
                    foodName = it.name,
                    quantity = it.quantity,
                    total = roundMoney(it.total),
                )
            }
            .sortedWith(
                compareByDescending<DailyReportFoodRow> { it.total }
                    .thenByDescending { it.quantity }
                    .thenBy { it.foodCode },
            )

        return DailyReportResponse(
            startDate = start,
            endDate = end,
            orderCount = touchIds.size,
            paidOrderCount = paidOrderCount,
            paidByQrScanOrderCount = paidByQrScanOrderCount,
            paidByCreditOrderCount = paidByCreditOrderCount,
            totalSales = roundMoney(totalSales),
            totalCashReceived = roundMoney(totalCashReceived),
            totalChange = roundMoney(totalChange),
            rows = rows,
            foods = foods,
        )
    }

    private data class FoodAgg(
        val code: String,
        val name: String,
        var quantity: Int,
        var total: Double,
    )

    private fun rollupFoodLines(order: PosOrder, into: MutableMap<Long, FoodAgg>) {
        for (line in order.lines) {
            if (resolvedLineStatus(line, order) == OrderLineStatus.CANCEL) {
                continue
            }
            val food = line.food ?: continue
            val fid = food.id ?: continue
            val lineTotal = line.quantity * line.unitPrice
            into.compute(fid) { _, existing ->
                if (existing == null) {
                    FoodAgg(food.code, food.name, line.quantity, lineTotal)
                } else {
                    existing.quantity += line.quantity
                    existing.total += lineTotal
                    existing
                }
            }
        }
    }

    private fun orderCreatedDate(order: PosOrder): LocalDate? =
        order.orderDate?.toLocalDate()

    private fun paidSettlementDate(order: PosOrder): LocalDate? {
        if (!order.paid) {
            return null
        }
        val t = order.paidAt ?: order.orderDate ?: return null
        return t.toLocalDate()
    }

    private fun inInclusiveRange(d: LocalDate, start: LocalDate, end: LocalDate): Boolean =
        !d.isBefore(start) && !d.isAfter(end)

    /** Aligns with front-end `resolvedLineStatus` for amount due. */
    private fun resolvedLineStatus(line: OrderLine, order: PosOrder): OrderLineStatus {
        when (line.status) {
            OrderLineStatus.CANCEL -> return OrderLineStatus.CANCEL
            OrderLineStatus.COMPLETE -> return OrderLineStatus.COMPLETE
            OrderLineStatus.WAIT, OrderLineStatus.FINISH_COOKING -> Unit
        }
        if (order.cancel) {
            return OrderLineStatus.CANCEL
        }
        if (order.complateOrder || order.paid) {
            return OrderLineStatus.COMPLETE
        }
        return OrderLineStatus.WAIT
    }

    private fun payableTotal(order: PosOrder): Double =
        order.lines.sumOf { line ->
            if (resolvedLineStatus(line, order) == OrderLineStatus.CANCEL) {
                0.0
            } else {
                line.quantity * line.unitPrice
            }
        }

    private fun roundMoney(v: Double): Double =
        kotlin.math.round(v * 100.0) / 100.0
}
