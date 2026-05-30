package me.pixka.pos.order.api

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class OrderRequest(
    /** When null or blank on create, the service assigns a unique number. */
    @field:Size(max = 255, message = "orderNo must be at most 255 characters")
    val orderNo: String? = null,

    @field:NotNull(message = "tableId is required")
    @field:JsonAlias("table_id")
    @param:JsonAlias("table_id")
    val tableId: Long,

    @field:NotNull(message = "orderDate is required")
    val orderDate: LocalDateTime,

    @field:NotNull(message = "complateOrder is required")
    val complateOrder: Boolean,

    val complateOrderDate: LocalDateTime?,

    @field:NotNull(message = "cancel is required")
    val cancel: Boolean,

    /** When null or omitted, treated as 0. */
    @field:PositiveOrZero(message = "paidPrice must be >= 0")
    val paidPrice: Double? = null,

    /** When null or omitted, treated as 0. */
    @field:PositiveOrZero(message = "change must be >= 0")
    val change: Double? = null,

    /** Optional whole-order note (not per-line kitchen note). Aliases match Angular/other clients. */
    @field:Size(max = 2000, message = "note must be at most 2000 characters")
    @field:JsonAlias("order_note", "orderNote")
    @param:JsonAlias("order_note", "orderNote")
    val note: String? = null,

    /** Operator who created the order; aliases match Angular JSON (`user_id`). */
    @field:JsonAlias("user_id")
    @param:JsonAlias("user_id")
    val userId: Long? = null,

    @field:NotEmpty(message = "lines must contain at least one item")
    @field:Valid
    val lines: List<OrderLineRequest>,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int
)
