package me.pixka.pos.order.service

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** ESC/POS kitchen chit: order + table + lines (food name, qty). ASCII only. */
object EscPosKitchenTicketBuilder {

    private const val WIDTH = 42

    fun build(
        orderNo: String,
        tableCode: String,
        kitchenName: String,
        items: List<Pair<String, Int>>,
    ): ByteArray {
        val out = ByteArrayOutputStream(1024)
        fun raw(vararg bytes: Int) {
            for (b in bytes) out.write(b)
        }
        fun line(s: String) {
            out.write(sanitize(s).toByteArray(StandardCharsets.US_ASCII))
            raw(0x0A)
        }

        raw(0x1B, 0x40)
        raw(0x1B, 0x61, 0x01)
        raw(0x1D, 0x21, 0x11)
        line("KITCHEN ORDER")
        raw(0x1D, 0x21, 0x00)
        raw(0x1B, 0x61, 0x00)

        line("Kitchen: $kitchenName")
        line("Order: $orderNo")
        line("Table: $tableCode")
        line("-".repeat(WIDTH))
        for ((name, qty) in items) {
            line("${qty} x ${sanitize(name)}")
        }
        line("-".repeat(WIDTH))
        raw(0x0A, 0x0A)
        raw(0x1B, 0x64, 0x03)
        raw(0x1D, 0x56, 0x01)
        return out.toByteArray()
    }

    private fun sanitize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when {
                ch == '\n' || ch == '\r' -> sb.append(' ')
                ch.code in 32..126 -> sb.append(ch)
                else -> sb.append('?')
            }
        }
        return sb.toString()
    }
}
