package me.pixka.pos.order.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EscPosKitchenTicketBuilderTest {

    @Test
    fun `build contains init and cut`() {
        val bytes = EscPosKitchenTicketBuilder.build(
            orderNo = "O-1",
            tableCode = "T1",
            kitchenName = "Hot",
            items = listOf("Burger" to 2, "Fries" to 1),
        )
        assertTrue(bytes.size > 40)
        assertEquals(0x1B, bytes[0].toInt() and 0xFF)
        assertEquals(0x40, bytes[1].toInt() and 0xFF)
        assertEquals(0x01, bytes.last().toInt() and 0xFF)
    }
}
