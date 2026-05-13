package me.pixka.pos.order.service

import me.pixka.pos.order.api.OrderReceipt
import me.pixka.pos.order.config.ReceiptPrinterProperties
import me.pixka.pos.order.exception.ReceiptPrinterDisabledException
import me.pixka.pos.order.exception.ReceiptPrinterMisconfiguredException
import org.springframework.stereotype.Service

@Service
class ReceiptTcpPrinterService(
    private val properties: ReceiptPrinterProperties,
    private val orderService: OrderService,
) {

    /** Loads the order receipt and sends ESC/POS to the configured printer. */
    fun printOrderReceipt(orderId: Long) {
        print(orderService.receipt(orderId))
    }

    fun print(receipt: OrderReceipt) {
        if (!properties.enabled) {
            throw ReceiptPrinterDisabledException(
                "Receipt printer is disabled (set app.receipt-printer.enabled=true and host)."
            )
        }
        val host = properties.host.trim()
        if (host.isEmpty()) {
            throw ReceiptPrinterMisconfiguredException(
                "app.receipt-printer.host is empty while printing is enabled."
            )
        }
        val payload = EscPosOrderReceiptBuilder.build(receipt)
        TcpEscPosTransport.send(
            host = host,
            port = properties.port,
            connectTimeoutMs = properties.connectTimeoutMs,
            readTimeoutMs = properties.readTimeoutMs,
            payload = payload,
        )
    }
}
