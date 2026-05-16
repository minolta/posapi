package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.Min

/**
 * Body for `PATCH /api/orders/{id}/note` — update whole-order note on any order (e.g. after payment).
 * [version] must match the current row for optimistic locking.
 */
data class PatchOrderNoteRequest(
    /** New note text; null or blank clears the stored note. Aliases match Angular PUT bodies. */
    @field:JsonAlias("order_note", "orderNote")
    @param:JsonAlias("order_note", "orderNote")
    val note: String?,
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int,
)
