package com.example.admin.identity.bootstrap

import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserStatus
import com.example.admin.identity.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSeederTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val properties: BootstrapAdminProperties,
) {

    private val args = DefaultApplicationArguments()

    private fun seeder(props: BootstrapAdminProperties = properties) =
        UserSeeder(userRepository, passwordEncoder, props)

    @BeforeEach
    fun emptyUsersTable() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `creates pending admin with hashed password when table is empty`() {
        seeder().run(args)

        val admin = assertNotNull(userRepository.findByEmail(properties.email))
        assertEquals(UserStatus.PENDING, admin.status)
        assertTrue(admin.mustChangePassword)
        assertTrue(admin.passwordHash.startsWith("{bcrypt}"))
        assertTrue(passwordEncoder.matches(properties.password, admin.passwordHash))
    }

    @Test
    fun `does not create second admin on repeated run`() {
        seeder().run(args)
        seeder().run(args)

        assertEquals(1, userRepository.count())
    }

    @Test
    fun `does nothing when table already has users`() {
        userRepository.save(User(email = "someone@example.com", passwordHash = "hash"))

        seeder().run(args)

        assertEquals(1, userRepository.count())
        assertNull(userRepository.findByEmail(properties.email))
    }

    @Test
    fun `normalizes email before saving`() {
        val props = BootstrapAdminProperties(email = "  Admin@Example.COM  ", password = "secret")

        seeder(props).run(args)

        assertNotNull(userRepository.findByEmail("admin@example.com"))
    }
}
