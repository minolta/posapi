package me.pixka.pos.order.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class OrderRequest(
    /** When null or blank on create, the service assigns a unique number. */
    @field:Size(max = 255, message = "orderNo must be at most 255 characters")
    val orderNo: String? = null,

    @field:NotNull(message = "tableId is required")
    val tableId: Long,

    @field:NotNull(message = "orderDate is required")
    val orderDate: LocalDateTime,

    @field:NotNull(message = "complateOrder is required")
    val complateOrder: Boolean,

    val complateOrderDate: LocalDateTime?,

    @field:NotNull(message = "cancel is required")
    val cancel: Boolean,

    @field:NotEmpty(message = "lines must contain at least one item")
    @field:Valid
    val lines: List<OrderLineRequest>,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int
)
