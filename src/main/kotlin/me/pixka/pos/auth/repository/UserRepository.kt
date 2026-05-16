package me.pixka.pos.auth.repository

import me.pixka.pos.auth.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsernameIgnoreCase(username: String): Optional<User>

    fun existsByUsernameIgnoreCase(username: String): Boolean

    fun findAllByOrderByUsernameAsc(): List<User>

    fun findByUsernameContainingIgnoreCaseOrderByUsernameAsc(username: String): List<User>
}
