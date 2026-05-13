package me.pixka.pos.order.service

import me.pixka.pos.order.api.OrderReceipt
import me.pixka.pos.order.api.ReceiptLineItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EscPosOrderReceiptBuilderTest {

    @Test
    fun `build starts with init and ends with feed and partial cut`() {
        val receipt = OrderReceipt(
            orderId = 1L,
            orderNo = "O1",
            orderDate = LocalDateTime.of(2026, 1, 1, 10, 0),
            tableCode = "T1",
            zoneName = "Z",
            cancel = false,
            lines = listOf(ReceiptLineItem("C", "Item", 1, 1.0, 1.0)),
            subtotal = 1.0,
            paidPrice = 5.0,
            change = 4.0,
            paid = false,
            paidAt = null,
        )
        val bytes = EscPosOrderReceiptBuilder.build(receipt)
        assertTrue(bytes.size > 80)
        assertEquals(0x1B, bytes[0].toInt() and 0xFF)
        assertEquals(0x40, bytes[1].toInt() and 0xFF)
        val end = bytes.copyOfRange(bytes.size - 6, bytes.size)
        assertEquals(listOf(0x1B, 0x64, 0x04, 0x1D, 0x56, 0x01), end.map { it.toInt() and 0xFF })
    }
}