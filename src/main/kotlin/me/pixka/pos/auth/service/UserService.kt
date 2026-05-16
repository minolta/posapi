package me.pixka.pos.auth.service

import me.pixka.pos.auth.api.RegisterRequest
import me.pixka.pos.auth.api.UserPatchRequest
import me.pixka.pos.auth.api.UserRequest
import me.pixka.pos.auth.api.UserResponse
import me.pixka.pos.auth.exception.InvalidCredentialsException
import me.pixka.pos.auth.exception.UserAlreadyExistsException
import me.pixka.pos.auth.exception.UserNotFoundException
import me.pixka.pos.auth.model.User
import me.pixka.pos.auth.model.UserRole
import me.pixka.pos.auth.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun list(q: String? = null): List<UserResponse> {
        val trimmed = q?.trim().orEmpty()
        val users = if (trimmed.isEmpty()) {
            userRepository.findAllByOrderByUsernameAsc()
        } else {
            userRepository.findByUsernameContainingIgnoreCaseOrderByUsernameAsc(trimmed)
        }
        return users.map { toResponse(it) }
    }

    @Transactional
    fun create(request: UserRequest): UserResponse {
        val password = request.password?.trim().orEmpty()
        if (password.length < 4) {
            throw IllegalArgumentException("password is required (min 4 characters)")
        }
        val username = request.username.trim()
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw UserAlreadyExistsException(username)
        }
        val user = User(
            username = username,
            passwordHash = passwordEncoder.encode(password)!!,
            role = request.role,
            enabled = request.enabled,
        )
        return toResponse(userRepository.save(user))
    }

    @Transactional
    fun register(request: RegisterRequest): User {
        val username = request.username.trim()
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw UserAlreadyExistsException(username)
        }
        val user = User(
            username = username,
            passwordHash = passwordEncoder.encode(request.password)!!,
            role = request.role ?: UserRole.STAFF,
        )
        return userRepository.save(user)
    }

    @Transactional
    fun update(id: Long, request: UserRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }
        val username = request.username.trim()
        val existing = userRepository.findByUsernameIgnoreCase(username)
        if (existing.isPresent && existing.get().id != id) {
            throw UserAlreadyExistsException(username)
        }
        user.username = username
        user.role = request.role
        user.enabled = request.enabled
        val password = request.password?.trim()
        if (!password.isNullOrEmpty()) {
            user.passwordHash = passwordEncoder.encode(password)!!
        }
        return toResponse(userRepository.save(user))
    }

    @Transactional
    fun patch(id: Long, request: UserPatchRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }
        request.password?.trim()?.takeIf { it.isNotEmpty() }?.let { pwd ->
            if (pwd.length < 4) {
                throw IllegalArgumentException("password must be at least 4 characters")
            }
            user.passwordHash = passwordEncoder.encode(pwd)!!
        }
        request.enabled?.let { user.enabled = it }
        request.roles?.firstOrNull()?.trim()?.uppercase()?.let { raw ->
            runCatching { UserRole.valueOf(raw) }.getOrNull()?.let { parsed -> user.role = parsed }
        }
        return toResponse(userRepository.save(user))
    }

    @Transactional
    fun delete(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }
        userRepository.deleteById(id)
    }

    fun authenticate(username: String, password: String): User {
        val user = userRepository.findByUsernameIgnoreCase(username.trim())
            .orElseThrow { InvalidCredentialsException() }
        if (!user.enabled || !passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return user
    }

    fun getById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }
        return toResponse(user)
    }

    fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            username = user.username,
            role = user.role,
            enabled = user.enabled,
            createdAt = user.createdAt,
        )
}
