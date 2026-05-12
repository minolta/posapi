package me.pixka.pos.foodcategory.api

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FoodCategoryRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    /** Optional display name; blank or omitted is stored as null. */
    val name: String? = null,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int,
)
