package me.pixka.pos.auth.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.pixka.pos.auth.model.UserRole
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val role: UserRole,
    val enabled: Boolean,
    @get:JsonProperty("active")
    val active: Boolean = enabled,
    val createdAt: LocalDateTime,
) {
    /** Single-role API; exposed as a list for compatibility with the POS Angular app. */
    val roles: List<String>
        get() = listOf(role.name)

    /** Optional display name — reserved for future use; SPA tolerates absent/null. */
    val displayName: String?
        get() = null
}
