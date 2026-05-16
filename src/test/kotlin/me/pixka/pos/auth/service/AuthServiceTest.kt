package me.pixka.pos.auth.service

import me.pixka.pos.auth.api.LoginRequest
import me.pixka.pos.auth.api.RegisterRequest
import me.pixka.pos.auth.exception.InvalidCredentialsException
import me.pixka.pos.auth.exception.UserAlreadyExistsException
import me.pixka.pos.auth.model.UserRole
import me.pixka.pos.auth.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {
    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun clearUsers() {
        userRepository.deleteAll()
    }

    @Test
    fun `register saves user and returns jwt`() {
        val response = authService.register(
            RegisterRequest(username = "cashier1", password = "secret123", role = UserRole.STAFF),
        )
        assertNotNull(response.token)
        assertEquals("Bearer", response.tokenType)
        assertEquals("cashier1", response.user.username)
        assertEquals(UserRole.STAFF, response.user.role)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `login returns jwt for valid credentials`() {
        authService.register(RegisterRequest(username = "admin2", password = "pass5678", role = UserRole.ADMIN))
        val response = authService.login(LoginRequest(username = "admin2", password = "pass5678"))
        assertNotNull(response.token)
        assertEquals("admin2", response.user.username)
    }

    @Test
    fun `login fails for wrong password`() {
        authService.register(RegisterRequest(username = "u1", password = "correct"))
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequest(username = "u1", password = "wrong"))
        }
    }

    @Test
    fun `register fails when username taken`() {
        authService.register(RegisterRequest(username = "dup", password = "pass1234"))
        assertThrows(UserAlreadyExistsException::class.java) {
            authService.register(RegisterRequest(username = "dup", password = "other5678"))
        }
    }
}
