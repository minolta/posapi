package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

/**
 * Optional body for `POST /api/orders/{id}/pay`.
 * Older clients omit the body; amounts then stay whatever was saved on the order (e.g. from PUT).
 *
 * `@param:[JsonAlias]` is required alongside `@field:[JsonAlias]` so Jackson binds JSON into the
 * Kotlin constructor reliably (Kotlin data classes).
 */
data class PayOrderRequest(
    @field:PositiveOrZero(message = "paidPrice must be >= 0")
    @field:JsonAlias("paid_price", "amountTendered", "amount_tendered")
    @param:JsonAlias("paid_price", "amountTendered", "amount_tendered")
    val paidPrice: Double? = null,

    @field:PositiveOrZero(message = "change must be >= 0")
    @field:JsonAlias("change_amount", "cashChange", "cash_change")
    @param:JsonAlias("change_amount", "cashChange", "cash_change")
    val change: Double? = null,

    /** When true, order is marked as paid by QR scan (saved on the order). */
    @field:JsonAlias("paid_by_qr_scan", "paidByScan", "payByQr", "confirmPayByScan")
    @param:JsonAlias("paid_by_qr_scan", "paidByQrScan", "paidByScan", "payByQr", "confirmPayByScan")
    val paidByQrScan: Boolean? = null,

    /** Optional raw payload from the scanned QR (e.g. PromptPay string). */
    @field:Size(max = 1024, message = "qrScanPayload must be at most 1024 characters")
    @field:JsonAlias("qr_scan_payload", "qrPayload", "qr_data")
    @param:JsonAlias("qr_scan_payload", "qrPayload", "qr_data", "qrScanPayload")
    val qrScanPayload: String? = null,
)
