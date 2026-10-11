package com.example.admin.identity.bootstrap

import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val properties: BootstrapAdminProperties
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(UserSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        if (userRepository.count() > 0) {
            log.info("Users table is not empty. Skipping.")
            return
        }

        val email = properties.email.trim().lowercase()

        val hash = passwordEncoder.encode(properties.password)!!
        val user = User(email = email, passwordHash = hash)

        userRepository.save(user)

        log.info("Created new user {}", email)
    }
}