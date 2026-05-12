package me.pixka.pos.zone.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ZoneRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotBlank(message = "name is required")
    val name: String,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int
)
