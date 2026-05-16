package me.pixka.pos.auth.service

import me.pixka.pos.auth.api.AuthResponse
import me.pixka.pos.auth.api.LoginRequest
import me.pixka.pos.auth.api.RegisterRequest
import me.pixka.pos.auth.api.UserResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userService: UserService,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val user = userService.register(request)
        return buildAuthResponse(user.id!!, userService.toResponse(user), jwtService.generateToken(user))
    }

    fun login(request: LoginRequest): AuthResponse {
        val entity = userService.authenticate(request.username, request.password)
        val user = userService.toResponse(entity)
        return buildAuthResponse(entity.id!!, user, jwtService.generateToken(entity))
    }

    fun me(userId: Long): UserResponse = userService.getById(userId)

    private fun buildAuthResponse(userId: Long, user: UserResponse, token: String): AuthResponse =
        AuthResponse(
            token = token,
            expiresInMs = jwtService.expirationMs,
            user = user.copy(id = userId),
        )
}
