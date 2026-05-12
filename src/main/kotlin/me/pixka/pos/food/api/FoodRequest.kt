package me.pixka.pos.food.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class FoodRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotBlank(message = "name is required")
    val name: String,

    @field:NotNull(message = "basePrice is required")
    @field:PositiveOrZero(message = "basePrice must be >= 0")
    val basePrice: Double,

    @field:NotNull(message = "kitchenId is required")
    val kitchenId: Long,

    @field:NotNull(message = "foodCategoryId is required")
    val foodCategoryId: Long,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int
)
