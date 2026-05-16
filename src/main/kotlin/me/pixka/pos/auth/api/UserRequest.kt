package me.pixka.pos.auth.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.pixka.pos.auth.model.UserRole

data class UserRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 64)
    val username: String,

    /** Required on create; omit or leave blank on update to keep the current password. */
    @field:Size(min = 4, max = 128)
    val password: String? = null,

    val role: UserRole = UserRole.STAFF,

    val enabled: Boolean = true,
)
