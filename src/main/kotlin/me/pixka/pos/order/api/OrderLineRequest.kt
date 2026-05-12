package me.pixka.pos.order.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import me.pixka.pos.order.model.OrderLineStatus

data class OrderLineRequest(
    @field:NotNull(message = "foodId is required")
    val foodId: Long,

    @field:NotNull(message = "quantity is required")
    @field:Min(value = 1, message = "quantity must be >= 1")
    val quantity: Int,

    /** Optional; defaults to WAIT when omitted by older clients. */
    val status: OrderLineStatus? = null
)
