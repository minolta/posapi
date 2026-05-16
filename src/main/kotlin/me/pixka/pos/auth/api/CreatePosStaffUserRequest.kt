package me.pixka.pos.auth.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import me.pixka.pos.auth.model.UserRole

/**
 * Create user body from the SPA (`POST /api/users`).
 * Maps to {@link UserRequest} internally.
 */
data class CreatePosStaffUserRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 64)
    val username: String,

    @field:NotBlank
    @field:Size(min = 4, max = 128)
    val password: String,

    /** Ignored until persisted server-side — accepted for forwards compatibility. */
    val displayName: String? = null,

    /** Legacy `UserRequest` JSON field; takes precedence over [roles] when present. */
    val role: UserRole? = null,

    val roles: List<String>? = null,

    /** Defaults to enabled when omitted. */
    val enabled: Boolean? = null,
)

fun CreatePosStaffUserRequest.toUserRequest(): UserRequest {
    val fromList = roles?.firstOrNull()?.trim()?.uppercase()?.let {
        runCatching { UserRole.valueOf(it) }.getOrNull()
    }
    val r = role ?: fromList ?: UserRole.STAFF

    return UserRequest(
        username = username.trim(),
        password = password,
        role = r,
        enabled = enabled ?: true,
    )
}
