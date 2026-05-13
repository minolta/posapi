package me.pixka.pos.order.service

import me.pixka.pos.order.api.OrderReceipt
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class OrderReceiptPdfService {

    private val fontTitle = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    private val fontBody = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun render(receipt: OrderReceipt): ByteArray {
        PDDocument().use { doc ->
            val margin = 54f
            val bottom = 56f
            val lineStep = 13f

            var page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            var cs = PDPageContentStream(doc, page)
            var y = page.mediaBox.height - margin

            fun newPage() {
                cs.close()
                page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                cs = PDPageContentStream(doc, page)
                y = page.mediaBox.height - margin
            }

            fun ensureSpace(extra: Float = lineStep) {
                if (y - extra < bottom) newPage()
            }

            fun write(text: String, size: Float = 10f, bold: Boolean = false) {
                ensureSpace()
                val font = if (bold) fontTitle else fontBody
                val safe = sanitizeWinAnsi(text)
                cs.beginText()
                cs.setFont(font, size)
                cs.newLineAtOffset(margin, y)
                cs.showText(safe)
                cs.endText()
                y -= lineStep
            }

            write("Receipt", 16f, bold = true)
            y -= 4f
            write("Order: ${receipt.orderNo}")
            write("Date: " + (receipt.orderDate?.format(dateFmt) ?: "-"))
            write("Table: ${receipt.tableCode}")
            write("Zone: " + (receipt.zoneName ?: "-"))
            if (receipt.cancel) write("CANCELLED", 11f, bold = true)
            y -= 6f
            write("Items", 9f, bold = true)
            y -= 2f

            for (item in receipt.lines) {
                val wrapped = wrapText("${item.quantity} x ${item.foodName} (${item.foodCode})", 72)
                wrapped.forEachIndexed { idx, ln ->
                    write(ln, if (idx == 0) 10f else 9f)
                }
                write("    @ ${money(item.unitPrice)}  =  ${money(item.lineTotal)}", 9f)
                y -= 2f
            }

            y -= 6f
            write("Subtotal: ${money(receipt.subtotal)}", 11f, bold = true)
            write("Paid: ${money(receipt.paidPrice)}")
            write("Change: ${money(receipt.change)}")
            write(if (receipt.paid) "Status: PAID" else "Status: unpaid", 10f, bold = receipt.paid)
            receipt.paidAt?.let { write("Paid at: ${it.format(dateFmt)}", 9f) }

            cs.close()
            return ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
        }
    }

    private fun money(v: Double): String = String.format(Locale.ROOT, "%.2f", v)

    private fun sanitizeWinAnsi(s: String): String {
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

    private fun wrapText(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val next = if (cur.isEmpty()) w else "${cur} $w"
            if (next.length <= maxChars) {
                cur.clear()
                cur.append(next)
            } else {
                if (cur.isNotEmpty()) lines.add(cur.toString())
                if (w.length > maxChars) {
                    var rest = w
                    while (rest.length > maxChars) {
                        lines.add(rest.take(maxChars))
                        rest = rest.drop(maxChars)
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
