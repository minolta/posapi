package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.pixka.pos.order.model.OrderLineStatus

data class OrderLineRequest(
    @field:NotNull(message = "foodId is required")
    @field:JsonAlias("food_id")
    @param:JsonAlias("food_id")
    val foodId: Long,

    @field:NotNull(message = "quantity is required")
    @field:Min(value = 1, message = "quantity must be >= 1")
    val quantity: Int,

    @field:JsonAlias("kitchen_note", "kitchenNote", "prep_note", "prepNote")
    @field:Size(max = 255, message = "note must be at most 255 characters")
    val note: String? = null,

    /** Optional; defaults to WAIT when omitted. Accepts FINISH_COOKING, CANSHIPNEW (legacy). */
    val status: OrderLineStatus? = null
)
