package me.pixka.pos.auth.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.pixka.pos.auth.model.UserRole

data class RegisterRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 64)
    val username: String,

    @field:NotBlank
    @field:Size(min = 4, max = 128)
    val password: String,

    val role: UserRole? = null,
)
