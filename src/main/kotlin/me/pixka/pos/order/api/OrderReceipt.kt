package me.pixka.pos.order.api

import java.time.LocalDateTime

data class ReceiptLineItem(
    val foodCode: String,
    val foodName: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
)

/** Printable / display snapshot for an order (line items, totals, table, payment). */
data class OrderReceipt(
    val orderId: Long,
    val orderNo: String,
    val orderDate: LocalDateTime?,
    val tableCode: String,
    val zoneName: String?,
    val orderNote: String?,
    val cancel: Boolean,
    val lines: List<ReceiptLineItem>,
    val subtotal: Double,
    val paidPrice: Double,
    val change: Double,
    val paid: Boolean,
    val paidAt: LocalDateTime?,
    val paidByQrScan: Boolean,
    val paidByCredit: Boolean,
    val qrScanPayload: String?,
)
