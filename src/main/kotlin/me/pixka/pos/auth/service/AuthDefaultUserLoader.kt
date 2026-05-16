package me.pixka.pos.auth.service

import me.pixka.pos.auth.model.User
import me.pixka.pos.auth.model.UserRole
import me.pixka.pos.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AuthDefaultUserLoader(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.auth.default-admin.enabled:true}")
    private val defaultAdminEnabled: Boolean,
    @Value("\${app.auth.default-admin.username:admin}")
    private val defaultAdminUsername: String,
    @Value("\${app.auth.default-admin.password:admin}")
    private val defaultAdminPassword: String,
) {
    private val log = LoggerFactory.getLogger(AuthDefaultUserLoader::class.java)

    @Bean
    fun loadDefaultAdminOnStartup(): ApplicationRunner = ApplicationRunner {
        if (!defaultAdminEnabled) {
            log.info("Default admin user disabled (app.auth.default-admin.enabled=false).")
            return@ApplicationRunner
        }
        val username = defaultAdminUsername.trim()
        if (username.isEmpty()) {
            log.warn("Default admin username is blank; skip seed.")
            return@ApplicationRunner
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.info("Default admin user '{}' already exists; skip seed.", username)
            return@ApplicationRunner
        }
        userRepository.save(
            User(
                username = username,
                passwordHash = passwordEncoder.encode(defaultAdminPassword)!!,
                role = UserRole.ADMIN,
            ),
        )
        log.info("Created default admin user '{}'.", username)
    }
}
