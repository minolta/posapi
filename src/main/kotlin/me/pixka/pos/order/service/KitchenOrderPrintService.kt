package me.pixka.pos.order.service

import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.order.api.KitchenPrintResponse
import me.pixka.pos.order.api.KitchenPrintResult
import me.pixka.pos.order.exception.OrderNotFoundException
import me.pixka.pos.order.exception.ReceiptPrinterIOException
import me.pixka.pos.order.model.OrderLine
import me.pixka.pos.order.repository.OrderRepository
import me.pixka.pos.printer.model.Printer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KitchenOrderPrintService(
    private val orderRepository: OrderRepository,
) {

    /**
     * Groups order lines by each line's food kitchen, merges quantities per food,
     * and sends one ESC/POS job per kitchen that has an enabled printer with a host.
     */
    @Transactional(readOnly = true)
    fun printOrderToKitchens(orderId: Long): KitchenPrintResponse {
        val order = orderRepository.findById(orderId).orElseThrow { OrderNotFoundException(orderId) }
        if (order.lines.isEmpty()) {
            return KitchenPrintResponse(
                results = emptyList(),
                message = "Order has no lines.",
            )
        }

        val rows = order.lines.mapNotNull { line -> kitchenRow(line) }
        if (rows.isEmpty()) {
            return KitchenPrintResponse(
                results = emptyList(),
                message = "No lines with a kitchen could be printed.",
            )
        }

        val byKitchen = rows.groupBy { it.kitchen.id!! }
        val results = mutableListOf<KitchenPrintResult>()

        for ((_, group) in byKitchen) {
            val kitchen = group.first().kitchen
            val printer = kitchen.printer
            when {
                printer == null ->
                    results.add(
                        KitchenPrintResult(
                            kitchen.id!!,
                            kitchen.name,
                            null,
                            false,
                            "No printer assigned to this kitchen.",
                        ),
                    )

                !printer.enabled ->
                    results.add(
                        KitchenPrintResult(
                            kitchen.id!!,
                            kitchen.name,
                            printer.code,
                            false,
                            "Printer is disabled.",
                        ),
                    )

                printer.host.isBlank() ->
                    results.add(
                        KitchenPrintResult(
                            kitchen.id!!,
                            kitchen.name,
                            printer.code,
                            false,
                            "Printer host is empty.",
                        ),
                    )

                else -> {
                    val items = mergeItems(group.map { it.line })
                    val payload = EscPosKitchenTicketBuilder.build(
                        orderNo = order.orderNo,
                        tableCode = order.table?.code ?: "-",
                        kitchenName = kitchen.name,
                        items = items,
                    )
                    try {
                        sendToPrinter(printer, payload)
                        results.add(
                            KitchenPrintResult(
                                kitchen.id!!,
                                kitchen.name,
                                printer.code,
                                true,
                                null,
                            ),
                        )
                    } catch (e: ReceiptPrinterIOException) {
                        results.add(
                            KitchenPrintResult(
                                kitchen.id!!,
                                kitchen.name,
                                printer.code,
                                false,
                                e.message,
                            ),
                        )
                    }
                }
            }
        }

        val printedAny = results.any { it.printed }
        val msg = when {
            printedAny -> null
            results.isEmpty() -> "Nothing to print."
            else -> "No ticket was sent; assign and enable a TCP printer on each kitchen, or check hosts."
        }
        return KitchenPrintResponse(results = results, message = msg)
    }

    private data class KitchenRow(val kitchen: Kitchen, val line: OrderLine)

    private fun kitchenRow(line: OrderLine): KitchenRow? {
        val food = line.food ?: return null
        val kitchen = food.kitchen ?: return null
        return KitchenRow(kitchen, line)
    }

    private fun mergeItems(lines: List<OrderLine>): List<Pair<String, Int>> {
        return lines
            .groupBy { it.food!!.id!! }
            .values
            .map { same ->
                val name = same.first().food!!.name
                val qty = same.sumOf { it.quantity }
                name to qty
            }
    }

    private fun sendToPrinter(printer: Printer, payload: ByteArray) {
        TcpEscPosTransport.send(
            host = printer.host,
            port = printer.port,
            connectTimeoutMs = printer.connectTimeoutMs,
            readTimeoutMs = printer.readTimeoutMs,
            payload = payload,
        )
    }
}
