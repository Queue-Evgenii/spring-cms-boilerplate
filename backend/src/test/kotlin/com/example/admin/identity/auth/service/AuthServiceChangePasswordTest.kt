package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.AuthEventType
import com.example.admin.identity.auth.model.ChangePasswordResult
import com.example.admin.identity.auth.model.NextStep
import com.example.admin.identity.auth.policy.PasswordViolation
import com.example.admin.identity.auth.repository.AuthEventRepository
import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserStatus
import com.example.admin.identity.user.model.UserType
import com.example.admin.identity.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceChangePasswordTest(
    @Autowired private val authService: AuthService,
    @Autowired private val loginChallengeService: LoginChallengeService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val authEventRepository: AuthEventRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
) {

    private val email = "change-password-test@example.com"
    private val oldPassword = "Old-passw0rd"
    private val newPassword = "New-Passw0rd!"

    @BeforeEach
    fun setUp() {
        cleanup()
    }

    @AfterEach
    fun tearDown() {
        cleanup()
    }

    private fun cleanup() {
        authEventRepository.deleteAll()
        userRepository.findByEmail(email)?.let { userRepository.delete(it) }
    }

    private fun createUser(
        status: UserStatus = UserStatus.ACTIVE,
        type: UserType = UserType.ADMIN,
    ): User {
        val user = User(email = email, passwordHash = passwordEncoder.encode(oldPassword)!!)
        user.status = status
        user.userType = type
        user.mustChangePassword = true
        return userRepository.save(user)
    }

    private fun tokenFor(user: User, step: NextStep = NextStep.CHANGE_PASSWORD) =
        loginChallengeService.issue(requireNotNull(user.id), step)

    private fun changePassword(token: String, password: String = newPassword) =
        authService.changePassword(token, password, ip = "127.0.0.1", userAgent = "test-agent")

    private fun reloadUser(): User = assertNotNull(userRepository.findByEmail(email))

    @Test
    fun `password is changed and next step is returned`() {
        val user = createUser()

        val result = assertIs<ChangePasswordResult.Success>(changePassword(tokenFor(user)))

        assertEquals(NextStep.TOTP_SETUP, result.nextStep)
        assertNotNull(result.stepToken)

        val reloaded = reloadUser()
        assertTrue(passwordEncoder.matches(newPassword, reloaded.passwordHash))
        assertFalse(passwordEncoder.matches(oldPassword, reloaded.passwordHash))
        assertFalse(reloaded.mustChangePassword)
        assertNotNull(reloaded.passwordChangedAt)
    }

    @Test
    fun `password change is written to auth events`() {
        val user = createUser()

        changePassword(tokenFor(user))

        val events = authEventRepository.findAll().filter { it.eventType == AuthEventType.PASSWORD_CHANGED }
        assertEquals(1, events.size)
        assertEquals(user.id, events.single().userId)
        assertEquals("127.0.0.1", events.single().ip)
    }

    @Test
    fun `returned token is valid for the next step`() {
        val user = createUser()

        val result = assertIs<ChangePasswordResult.Success>(changePassword(tokenFor(user)))

        assertEquals(user.id, loginChallengeService.consume(assertNotNull(result.stepToken), NextStep.TOTP_SETUP))
    }

    @Test
    fun `weak password is rejected and token stays valid`() {
        val user = createUser()
        val token = tokenFor(user)

        val result = assertIs<ChangePasswordResult.WeakPassword>(changePassword(token, "Password!"))

        assertEquals(listOf(PasswordViolation.NO_DIGIT), result.violations)
        assertTrue(passwordEncoder.matches(oldPassword, reloadUser().passwordHash))
        assertEquals(user.id, loginChallengeService.consume(token, NextStep.CHANGE_PASSWORD))
    }

    @Test
    fun `unknown token is rejected`() {
        createUser()

        assertEquals(ChangePasswordResult.InvalidToken, changePassword("not-a-real-token"))
    }

    @Test
    fun `token cannot be used twice`() {
        val token = tokenFor(createUser())

        assertIs<ChangePasswordResult.Success>(changePassword(token))
        assertEquals(ChangePasswordResult.InvalidToken, changePassword(token, "Another-Passw0rd!"))
    }

    @Test
    fun `token issued for another step is rejected`() {
        val token = tokenFor(createUser(), NextStep.TOTP_REQUIRED)

        assertEquals(ChangePasswordResult.InvalidToken, changePassword(token))
        assertTrue(passwordEncoder.matches(oldPassword, reloadUser().passwordHash))
    }

    @Test
    fun `blocked user cannot change password`() {
        val token = tokenFor(createUser(status = UserStatus.BLOCKED))

        assertEquals(ChangePasswordResult.InvalidToken, changePassword(token))
        assertTrue(passwordEncoder.matches(oldPassword, reloadUser().passwordHash))
    }

    @Test
    fun `customer cannot change password through admin flow`() {
        val token = tokenFor(createUser(type = UserType.CUSTOMER))

        assertEquals(ChangePasswordResult.InvalidToken, changePassword(token))
        assertNull(reloadUser().passwordChangedAt)
    }
}
