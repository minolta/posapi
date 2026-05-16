package me.pixka.pos.order.service

import me.pixka.pos.order.api.OrderReceipt
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ESC/POS bytes for Epson TM-T82 (and similar) over raw TCP (port 9100).
 * Uses US-ASCII only so default printer code pages do not reject bytes.
 */
object EscPosOrderReceiptBuilder {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private const val WIDTH = 42

    fun build(receipt: OrderReceipt): ByteArray {
        val out = ByteArrayOutputStream(2048)
        fun raw(vararg bytes: Int) {
            for (b in bytes) out.write(b)
        }

        fun textLine(s: String) {
            out.write(sanitizeAscii(s).toByteArray(StandardCharsets.US_ASCII))
            raw(0x0A)
        }

        // ESC @ init
        raw(0x1B, 0x40)
        // center, double size title
        raw(0x1B, 0x61, 0x01)
        raw(0x1D, 0x21, 0x11)
        textLine("RECEIPT")
        raw(0x1D, 0x21, 0x00)
        raw(0x1B, 0x61, 0x00)

        textLine("Order: ${receipt.orderNo}")
        textLine("Date: " + (receipt.orderDate?.format(dateFmt) ?: "-"))
        textLine("Table: ${receipt.tableCode}")
        textLine("Zone: " + (receipt.zoneName ?: "-"))
        receipt.orderNote?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
            raw(0x1B, 0x45, 0x01)
            textLine("ORDER NOTE")
            raw(0x1B, 0x45, 0x00)
            for (ln in wrap(note, WIDTH)) {
                textLine(ln)
            }
        }
        if (receipt.cancel) {
            raw(0x1B, 0x45, 0x01)
            textLine("*** CANCELLED ***")
            raw(0x1B, 0x45, 0x00)
        }
        textLine("-".repeat(WIDTH))
        textLine("ITEMS")
        textLine("-".repeat(WIDTH))

        for (item in receipt.lines) {
            for (ln in wrap("${item.quantity} x ${item.foodName} (${item.foodCode})", WIDTH)) {
                textLine(ln)
            }
            textLine("  @ ${money(item.unitPrice)} = ${money(item.lineTotal)}")
        }

        textLine("-".repeat(WIDTH))
        raw(0x1B, 0x45, 0x01)
        textLine("Subtotal: ${money(receipt.subtotal)}")
        raw(0x1B, 0x45, 0x00)
        textLine("Paid: ${money(receipt.paidPrice)}")
        textLine("Change: ${money(receipt.change)}")
        if (receipt.paid) {
            raw(0x1B, 0x45, 0x01)
            textLine("Status: PAID")
            raw(0x1B, 0x45, 0x00)
        } else {
            textLine("Status: unpaid")
        }
        receipt.paidAt?.let { textLine("Paid at: ${it.format(dateFmt)}") }
        when {
            receipt.paidByQrScan -> {
                raw(0x1B, 0x45, 0x01)
                textLine("Payment: QR scan")
                raw(0x1B, 0x45, 0x00)
                receipt.qrScanPayload?.let { p ->
                    val short = if (p.length > 80) p.take(80) + "..." else p
                    textLine("QR ref: $short")
                }
            }
            receipt.paidByCredit -> {
                raw(0x1B, 0x45, 0x01)
                textLine("Payment: Credit card")
                raw(0x1B, 0x45, 0x00)
            }
        }

        raw(0x0A, 0x0A, 0x0A)
        // Feed then partial cut (GS V 1) — common on Epson TM series
        raw(0x1B, 0x64, 0x04)
        raw(0x1D, 0x56, 0x01)

        return out.toByteArray()
    }

    private fun money(v: Double): String = String.format(Locale.ROOT, "%.2f", v)

    private fun sanitizeAscii(s: String): String {
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

    private fun wrap(text: String, max: Int): List<String> {
        val t = sanitizeAscii(text)
        if (t.length <= max) return listOf(t)
        val words = t.split(' ')
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val next = if (cur.isEmpty()) w else "${cur} $w"
            if (next.length <= max) {
                cur.clear()
                cur.append(next)
            } else {
                if (cur.isNotEmpty()) lines.add(cur.toString())
                if (w.length > max) {
                    var rest = w
                    while (rest.length > max) {
                        lines.add(rest.take(max))
                        rest = rest.drop(max)
                    }
                    cur = StringBuilder(rest)
                } else {
                    cur = StringBuilder(w)
                }
            }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines
    }
}
