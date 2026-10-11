package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.AuthEvent
import com.example.admin.identity.auth.model.AuthEventType
import com.example.admin.identity.auth.model.AuthFailReason
import com.example.admin.identity.auth.model.LoginResult
import com.example.admin.identity.auth.model.NextStep
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


@TestConfiguration
class AuthServiceTestConfig {
    @Bean
    @Primary
    fun testClock(): MutableClock = MutableClock(START_TIME)

    companion object {
        val START_TIME: Instant = Instant.parse("2030-01-01T10:00:00Z")
    }
}


@SpringBootTest(
    properties = [
        "app.auth.max-failed-attempts=3",
        "app.auth.lock-duration=10m",
    ],
)
@ActiveProfiles("test")
@Import(AuthServiceTestConfig::class)
class AuthServiceTest(
    @Autowired private val authService: AuthService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val authEventRepository: AuthEventRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val clock: MutableClock,
) {

    private val email = "auth-test@example.com"
    private val password = "correct-password"
    private val wrongPassword = "wrong-password"

    @BeforeEach
    fun setUp() {
        cleanup()
        clock.set(AuthServiceTestConfig.START_TIME)
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
        mustChangePassword: Boolean = false,
        totpEnabled: Boolean = false,
    ): User {
        val user = User(email = email, passwordHash = passwordEncoder.encode(password)!!)
        user.status = status
        user.userType = type
        user.mustChangePassword = mustChangePassword
        if (totpEnabled) user.totpEnabledAt = clock.instant()
        return userRepository.save(user)
    }

    private fun login(pass: String = password, loginEmail: String = email) =
        authService.login(loginEmail, pass, ip = "127.0.0.1", userAgent = "test-agent")

    private fun reloadUser(): User = assertNotNull(userRepository.findByEmail(email))

    private fun eventsOf(type: AuthEventType): List<AuthEvent> =
        authEventRepository.findAll().filter { it.eventType == type }

    @Test
    fun `wrong password increments failed attempts and logs event`() {
        createUser()

        val result = login(wrongPassword)

        assertEquals(LoginResult.Failed, result)
        assertEquals(1, reloadUser().failedLoginAttempts)

        val failed = eventsOf(AuthEventType.LOGIN_FAILED)
        assertEquals(1, failed.size)
        assertEquals(AuthFailReason.BAD_PASSWORD, failed.single().reason)
        assertEquals(email, failed.single().emailAttempted)
        assertEquals("127.0.0.1", failed.single().ip)
    }

    @Test
    fun `account is locked after max failed attempts`() {
        createUser()

        repeat(3) { login(wrongPassword) }

        val user = reloadUser()
        assertEquals(clock.instant().plus(Duration.ofMinutes(10)), user.lockedUntil)
        assertEquals(0, user.failedLoginAttempts)
        assertEquals(1, eventsOf(AuthEventType.ACCOUNT_LOCKED).size)

       val attemptNumbers = eventsOf(AuthEventType.LOGIN_FAILED)
            .filter { it.reason == AuthFailReason.BAD_PASSWORD }
            .map { it.details?.get("failedAttempts") }
            .toSet()
        assertEquals(setOf<Any?>(1, 2, 3), attemptNumbers)
    }

    @Test
    fun `correct password is rejected while account is locked`() {
        createUser()
        repeat(3) { login(wrongPassword) }

        val result = login(password)

        assertEquals(LoginResult.Failed, result)
        val reasons = eventsOf(AuthEventType.LOGIN_FAILED).map { it.reason }
        assertTrue(AuthFailReason.LOCKED in reasons)
    }

    @Test
    fun `login works again after lock expires`() {
        createUser()
        repeat(3) { login(wrongPassword) }

        clock.advance(Duration.ofMinutes(10).plusSeconds(1))

        val result = login(password)

        assertTrue(result is LoginResult.Success)
    }

    @Test
    fun `successful login resets failed attempts`() {
        createUser()
        login(wrongPassword)
        login(wrongPassword)
        assertEquals(2, reloadUser().failedLoginAttempts)

        login(password)

        val user = reloadUser()
        assertEquals(0, user.failedLoginAttempts)
        assertNull(user.lockedUntil)
        assertEquals(1, eventsOf(AuthEventType.LOGIN_SUCCESS).size)
    }

    @Test
    fun `unknown email and wrong password give the same result`() {
        createUser()

        val unknown = login(loginEmail = "nobody@example.com")
        val badPassword = login(wrongPassword)

        assertEquals(unknown, badPassword)
        val reasons = eventsOf(AuthEventType.LOGIN_FAILED).map { it.reason }
        assertTrue(AuthFailReason.UNKNOWN_EMAIL in reasons)
        assertTrue(AuthFailReason.BAD_PASSWORD in reasons)
    }

    @Test
    fun `blocked user cannot log in`() {
        createUser(status = UserStatus.BLOCKED)

        assertEquals(LoginResult.Failed, login())
        assertEquals(AuthFailReason.BLOCKED, eventsOf(AuthEventType.LOGIN_FAILED).single().reason)
    }

    @Test
    fun `customer cannot use admin login`() {
        createUser(type = UserType.CUSTOMER)

        assertEquals(LoginResult.Failed, login())
        assertEquals(AuthFailReason.NOT_ALLOWED, eventsOf(AuthEventType.LOGIN_FAILED).single().reason)
    }

    @Test
    fun `email is normalized on login`() {
        createUser()

        val result = login(loginEmail = "  AUTH-Test@Example.COM  ")

        assertTrue(result is LoginResult.Success)
    }

    @Test
    fun `fresh admin must change password first`() {
        createUser(mustChangePassword = true)

        assertEquals(LoginResult.Success(NextStep.CHANGE_PASSWORD), login())
    }

    @Test
    fun `admin without totp is sent to totp setup`() {
        createUser(mustChangePassword = false, totpEnabled = false)

        assertEquals(LoginResult.Success(NextStep.TOTP_SETUP), login())
    }

    @Test
    fun `admin with totp is asked for totp code`() {
        createUser(mustChangePassword = false, totpEnabled = true)

        assertEquals(LoginResult.Success(NextStep.TOTP_REQUIRED), login())
    }
}
