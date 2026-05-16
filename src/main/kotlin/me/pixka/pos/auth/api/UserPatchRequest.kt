package me.pixka.pos.auth.api

import jakarta.validation.constraints.Size

/** Partial update aligned with SPA `PATCH /api/users/{id}`. All fields optional. */
data class UserPatchRequest(
    @field:Size(min = 4, max = 128)
    val password: String? = null,
    /** First entry wins — `ADMIN`, `STAFF`, case-insensitive. */
    val roles: List<String>? = null,
    val enabled: Boolean? = null,
    /** Accepted for forwards compatibility; ignored by the server until a column exists. */
    val displayName: String? = null,
)
