package me.pixka.pos.kitchen.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class KitchenRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotBlank(message = "name is required")
    val name: String,

    /** When set, tickets for this kitchen print to this printer (TCP). Null clears assignment. */
    val printerId: Long? = null,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int
)
