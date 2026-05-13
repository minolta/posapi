package me.pixka.pos.order.service

import me.pixka.pos.food.exception.FoodNotFoundException
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.order.api.OrderLineRequest
import me.pixka.pos.order.api.PayOrderRequest
import me.pixka.pos.order.api.OrderReport
import me.pixka.pos.order.api.OrderReceipt
import me.pixka.pos.order.api.OrderRequest
import me.pixka.pos.order.api.DailyOrderReport
import me.pixka.pos.order.api.ReportLineItem
import me.pixka.pos.order.api.ReceiptLineItem
import me.pixka.pos.order.exception.OrderAlreadyPaidException
import me.pixka.pos.order.exception.OrderNotFoundException
import me.pixka.pos.order.model.OrderLine
import me.pixka.pos.order.model.OrderLineStatus
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.table.exception.TableNotFoundException
import me.pixka.pos.table.repository.TableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository,
    private val foodRepository: FoodRepository
) {
    fun create(request: OrderRequest): PosOrder {
        val table = tableRepository.findById(request.tableId).orElseThrow { TableNotFoundException(request.tableId) }
        val order = PosOrder(
            orderNo = resolveOrderNoForCreate(request.orderNo, request.orderDate),
            paidPrice = request.paidPrice ?: 0.0,
            change = request.change ?: 0.0,
            table = table,
            orderDate = request.orderDate,
            complateOrder = request.complateOrder,
            complateOrderDate = request.complateOrderDate,
            cancel = request.cancel,
            paid = false,
            paidAt = null
        )
        replaceLines(order, request.lines)
        return orderRepository.save(order)
    }

    fun update(id: Long, request: OrderRequest): PosOrder {
        val order = orderRepository.findById(id).orElseThrow { OrderNotFoundException(id) }
        assertOrderOpen(order)
        val table = tableRepository.findById(request.tableId).orElseThrow { TableNotFoundException(request.tableId) }

        val newNo = request.orderNo?.trim().orEmpty()
        if (newNo.isNotEmpty()) {
            order.orderNo = newNo
        }
        order.table = table
        order.orderDate = request.orderDate
        order.complateOrder = request.complateOrder
        order.complateOrderDate = request.complateOrderDate
        order.cancel = request.cancel
        order.paidPrice = request.paidPrice ?: 0.0
        order.change = request.change ?: 0.0
        replaceLines(order, request.lines)
        return orderRepository.save(order)
    }

    fun pay(id: Long, payment: PayOrderRequest? = null): PosOrder {
        val order = orderRepository.findById(id).orElseThrow { OrderNotFoundException(id) }
        if (order.paid) {
            throw OrderAlreadyPaidException(id)
        }
        payment?.paidPrice?.let { order.paidPrice = kotlin.math.max(0.0, it) }
        payment?.change?.let { order.change = kotlin.math.max(0.0, it) }
        order.paid = true
        order.paidAt = LocalDateTime.now()
        order.complateOrder = true
        order.complateOrderDate = LocalDateTime.now()
        return orderRepository.save(order)
    }

    fun delete(id: Long) {
        if (!orderRepository.existsById(id)) {
            throw OrderNotFoundException(id)
        }
        orderRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun receipt(id: Long): OrderReceipt {
        val order = orderRepository.findById(id).orElseThrow { OrderNotFoundException(id) }
        val table = order.table ?: throw IllegalStateException("order ${order.id} has no table")
        val lineItems = order.lines.map { line ->
            val food = line.food ?: throw IllegalStateException("order line ${line.id} has no food")
            val lineTotal = line.quantity * line.unitPrice
            ReceiptLineItem(
                foodCode = food.code,
                foodName = food.name,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                lineTotal = lineTotal,
            )
        }
        val subtotal = lineItems.sumOf { it.lineTotal }
        return OrderReceipt(
            orderId = order.id!!,
            orderNo = order.orderNo,
            orderDate = order.orderDate,
            tableCode = table.code,
            zoneName = table.zone?.name,
            cancel = order.cancel,
            lines = lineItems,
            subtotal = subtotal,
            paidPrice = order.paidPrice,
            change = order.change,
            paid = order.paid,
            paidAt = order.paidAt,
        )
    }

    fun search(q: String?): List<PosOrder> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            orderRepository.findAll()
        } else {
            orderRepository.findByOrderNoContainingIgnoreCaseOrderByOrderNoAsc(trimmed)
        }
    }

    @Transactional(readOnly = true)
    fun report(startDate: LocalDate?, endDate: LocalDate?): OrderReport {
        val start = startDate ?: LocalDate.now()
        val end = endDate ?: LocalDate.now()
        require(!end.isBefore(start)) { "endDate must be on or after startDate" }

        val from = start.atStartOfDay()
        val to = end.plusDays(1).atStartOfDay().minusNanos(1)
        val orders = orderRepository.findByOrderDateBetweenOrderByOrderDateAsc(from, to)

        val daily = orders
            .groupBy { it.orderDate!!.toLocalDate() }
            .toSortedMap()
            .map { (date, list) ->
                DailyOrderReport(
                    date = date,
                    orderCount = list.size,
                    paidOrderCount = list.count { it.paid },
                    cancelledOrderCount = list.count { it.cancel },
                    grossSales = list.filter { it.paid && !it.cancel }.sumOf { order ->
                        order.lines.sumOf { it.quantity * it.unitPrice }
                    },
                )
            }

        val items = orders
            .asSequence()
            .flatMap { it.lines.asSequence() }
            .filter { it.order?.paid == true && it.order?.cancel == false }
            .groupBy { it.food!!.id!! }
            .values
            .map { lines ->
                val first = lines.first()
                val food = first.food!!
                ReportLineItem(
                    foodCode = food.code,
                    foodName = food.name,
                    quantity = lines.sumOf { it.quantity },
                    total = lines.sumOf { it.quantity * it.unitPrice },
                )
            }
            .sortedBy { it.foodCode }

        return OrderReport(
            startDate = start,
            endDate = end,
            orderCount = orders.size,
            paidOrderCount = orders.count { it.paid },
            cancelledOrderCount = orders.count { it.cancel },
            grossSales = items.sumOf { it.total },
            daily = daily,
            items = items,
        )
    }

    private fun assertOrderOpen(order: PosOrder) {
        if (order.paid) {
            throw OrderAlreadyPaidException(order.id!!)
        }
    }

    private fun resolveOrderNoForCreate(clientOrderNo: String?, orderDate: LocalDateTime): String {
        val trimmed = clientOrderNo?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            return trimmed
        }
        return newGeneratedOrderNo(orderDate)
    }

    private fun newGeneratedOrderNo(orderDate: LocalDateTime): String {
        val dayPart = orderDate.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
        val prefix = "$dayPart-"
        var seq = orderRepository.countByOrderNoStartingWith(prefix) + 1
        var candidate = "%s%03d".format(prefix, seq)
        while (orderRepository.existsByOrderNo(candidate)) {
            seq += 1
            candidate = "%s%03d".format(prefix, seq)
        }
        return candidate
    }

    private fun replaceLines(order: PosOrder, lineRequests: List<OrderLineRequest>) {
        order.lines.clear()
        for (lr in lineRequests) {
            val food = foodRepository.findById(lr.foodId).orElseThrow { FoodNotFoundException(lr.foodId) }
            order.lines.add(
                OrderLine(
                    order = order,
                    food = food,
                    quantity = lr.quantity,
                    note = lr.note?.trim()?.takeIf { it.isNotEmpty() },
                    unitPrice = food.basePrice,
                    status = lr.status ?: OrderLineStatus.WAIT
                )
            )
        }
    }
}
