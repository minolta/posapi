package me.pixka.pos.kitchen.api

import jakarta.validation.constraints.NotBlank

data class NewKitchenRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotBlank(message = "name is required")
    val name: String
)
