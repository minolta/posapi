package me.pixka.pos.table.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class TableRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotNull(message = "basePrice is required")
    @field:PositiveOrZero(message = "basePrice must be >= 0")
    val basePrice: Double,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int,

    @field:NotNull(message = "zoneId is required")
    val zoneId: Long
)
