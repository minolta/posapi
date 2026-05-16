package me.pixka.pos.auth.api

import com.fasterxml.jackson.annotation.JsonGetter

data class AuthResponse(
    /** JWT string (HS256); also exposed as {@link #accessToken} for SPA clients. */
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInMs: Long,
    val user: UserResponse,
) {
    /** Same as [token]; many frontends expect this key from `/api/auth/login`. */
    val accessToken: String
        get() = token

    @JsonGetter("access_token")
    fun accessTokenSnakeCase(): String = token
}
