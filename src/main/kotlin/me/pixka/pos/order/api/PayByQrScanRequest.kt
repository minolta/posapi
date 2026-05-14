package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

/**
 * Body for `POST /api/orders/{id}/pay/qr-scan` — confirm settlement after scanning a payment QR (e.g. PromptPay slip).
 * Optional amounts default to whatever the client saved on the order via PUT (usually already equal to due).
 */
data class PayByQrScanRequest(
    @field:NotBlank(message = "qrScanPayload is required")
    @field:Size(max = 1024, message = "qrScanPayload must be at most 1024 characters")
    @field:JsonAlias("qr_scan_payload", "qrPayload", "qr_data")
    val qrScanPayload: String,

    @field:PositiveOrZero(message = "paidPrice must be >= 0")
    @field:JsonAlias("paid_price", "amountTendered", "amount_tendered")
    val paidPrice: Double? = null,

    @field:PositiveOrZero(message = "change must be >= 0")
    @field:JsonAlias("change_amount", "cashChange", "cash_change")
    val change: Double? = null,
)
