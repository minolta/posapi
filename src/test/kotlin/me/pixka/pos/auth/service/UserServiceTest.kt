package me.pixka.pos.auth.service

import me.pixka.pos.auth.api.UserRequest
import me.pixka.pos.auth.exception.UserNotFoundException
import me.pixka.pos.auth.model.UserRole
import me.pixka.pos.auth.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun clearUsers() {
        userRepository.deleteAll()
    }

    @Test
    fun `list returns all users`() {
        userService.create(UserRequest(username = "a1", password = "pass1234", role = UserRole.STAFF))
        userService.create(UserRequest(username = "b2", password = "pass5678", role = UserRole.ADMIN))
        val users = userService.list()
        assertEquals(2, users.size)
        assertEquals("a1", users[0].username)
        assertEquals("b2", users[1].username)
    }

    @Test
    fun `update can disable user`() {
        val created = userService.create(UserRequest(username = "u1", password = "pass1234"))
        val updated = userService.update(
            created.id,
            UserRequest(username = "u1", password = null, role = UserRole.STAFF, enabled = false),
        )
        assertFalse(updated.enabled)
        assertFalse(updated.active)
    }

    @Test
    fun `delete removes user`() {
        val created = userService.create(UserRequest(username = "del", password = "pass1234"))
        userService.delete(created.id)
        assertThrows(UserNotFoundException::class.java) {
            userService.getById(created.id)
        }
    }

    @Test
    fun `search filters by username`() {
        userService.create(UserRequest(username = "cashier", password = "pass1234"))
        userService.create(UserRequest(username = "manager", password = "pass5678"))
        val found = userService.list("cash")
        assertEquals(1, found.size)
        assertEquals("cashier", found[0].username)
        assertTrue(found[0].enabled)
    }
}
