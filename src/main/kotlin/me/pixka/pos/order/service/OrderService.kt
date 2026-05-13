package me.pixka.pos.order.service

import me.pixka.pos.food.exception.FoodNotFoundException
import me.pixka.pos.food.repository.FoodRepository
import me.pixka.pos.order.api.OrderLineRequest
import me.pixka.pos.order.api.OrderRequest
import me.pixka.pos.order.exception.OrderAlreadyPaidException
import me.pixka.pos.order.exception.OrderNotFoundException
import me.pixka.pos.order.model.OrderLine
import me.pixka.pos.order.model.OrderLineStatus
import me.pixka.pos.order.model.PosOrder
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.table.exception.TableNotFoundException
import me.pixka.pos.table.repository.TableRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    fun pay(id: Long): PosOrder {
        val order = orderRepository.findById(id).orElseThrow { OrderNotFoundException(id) }
        if (order.paid) {
            throw OrderAlreadyPaidException(id)
        }
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

    fun search(q: String?): List<PosOrder> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            orderRepository.findAll()
        } else {
            orderRepository.findByOrderNoContainingIgnoreCaseOrderByOrderNoAsc(trimmed)
        }
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
                    unitPrice = food.basePrice,
                    status = lr.status ?: OrderLineStatus.WAIT
                )
            )
        }
    }
}
