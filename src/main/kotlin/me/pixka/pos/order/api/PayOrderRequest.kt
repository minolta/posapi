package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.PositiveOrZero

/**
 * Optional body for `POST /api/orders/{id}/pay`.
 * Older clients omit the body; amounts then stay whatever was saved on the order (e.g. from PUT).
 */
data class PayOrderRequest(
    @field:PositiveOrZero(message = "paidPrice must be >= 0")
    @field:JsonAlias("paid_price", "amountTendered", "amount_tendered")
    val paidPrice: Double? = null,

    @field:PositiveOrZero(message = "change must be >= 0")
    @field:JsonAlias("change_amount", "cashChange", "cash_change")
    val change: Double? = null,
)
