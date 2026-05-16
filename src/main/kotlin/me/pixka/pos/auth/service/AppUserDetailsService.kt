package me.pixka.pos.auth.service

import me.pixka.pos.auth.exception.UserNotFoundByUsernameException
import me.pixka.pos.auth.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsernameIgnoreCase(username.trim())
            .orElseThrow { UserNotFoundByUsernameException(username) }
        return User.builder()
            .username(user.username)
            .password(user.passwordHash)
            .disabled(!user.enabled)
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .build()
    }
}
