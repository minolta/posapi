package me.pixka.pos.order.service

import me.pixka.pos.order.api.OrderReceipt
import me.pixka.pos.order.api.ReceiptLineItem
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OrderReceiptPdfServiceTest {

    private val pdfService = OrderReceiptPdfService()

    @Test
    fun `render produces PDF with valid header`() {
        val receipt = OrderReceipt(
            orderId = 1L,
            orderNo = "ORD-TEST",
            orderDate = LocalDateTime.of(2026, 5, 13, 12, 30),
            tableCode = "T1",
            zoneName = "Main",
            cancel = false,
            lines = listOf(
                ReceiptLineItem("F1", "Burger", 2, 5.5, 11.0),
                ReceiptLineItem("F2", "Water", 1, 2.0, 2.0),
            ),
            subtotal = 13.0,
            paidPrice = 20.0,
            change = 7.0,
            paid = true,
            paidAt = LocalDateTime.of(2026, 5, 13, 12, 35),
        )
        val bytes = pdfService.render(receipt)
        assertTrue(bytes.size > 200, "PDF should have non-trivial size")
        val header = bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII)
        assertTrue(header.startsWith("%PDF"), "expected PDF magic, got: $header")
    }
}
