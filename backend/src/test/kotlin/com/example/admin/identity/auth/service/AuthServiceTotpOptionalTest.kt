package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.ChangePasswordResult
import com.example.admin.identity.auth.model.LoginResult
import com.example.admin.identity.auth.model.NextStep
import com.example.admin.identity.auth.repository.AuthEventRepository
import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserType
import com.example.admin.identity.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest(properties = ["app.auth.totp-required=false"])
@ActiveProfiles("test")
class AuthServiceTotpOptionalTest(
    @Autowired private val authService: AuthService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val authEventRepository: AuthEventRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val loginChallengeService: LoginChallengeService,
) {

    private val email = "totp-optional-test@example.com"
    private val password = "correct-password"

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

    private fun createAdmin(totpEnabled: Boolean, mustChangePassword: Boolean = false) {
        val user = User(email = email, passwordHash = passwordEncoder.encode(password)!!)
        user.userType = UserType.ADMIN
        user.mustChangePassword = mustChangePassword
        if (totpEnabled) user.totpEnabledAt = Instant.now()
        userRepository.save(user)
    }

    private fun login() = authService.login(email, password, ip = "127.0.0.1", userAgent = "test-agent")

    @Test
    fun `login completes when totp is not required and not configured`() {
        createAdmin(totpEnabled = false)

        assertEquals(LoginResult.Success(NextStep.COMPLETE, stepToken = null), login())
    }

    @Test
    fun `configured totp is still requested when it is not mandatory`() {
        createAdmin(totpEnabled = true)

        assertEquals(NextStep.TOTP_REQUIRED, assertIs<LoginResult.Success>(login()).nextStep)
    }

    @Test
    fun `password change still comes first`() {
        createAdmin(totpEnabled = false, mustChangePassword = true)

        assertEquals(NextStep.CHANGE_PASSWORD, assertIs<LoginResult.Success>(login()).nextStep)
    }

    @Test
    fun `changing password completes login when totp is not required`() {
        createAdmin(totpEnabled = false, mustChangePassword = true)
        val userId = requireNotNull(userRepository.findByEmail(email)?.id)
        val token = loginChallengeService.issue(userId, NextStep.CHANGE_PASSWORD)

        val result = authService.changePassword(token, "New-Passw0rd!", ip = "127.0.0.1", userAgent = "test-agent")

        assertEquals(ChangePasswordResult.Success(NextStep.COMPLETE, stepToken = null), result)
    }
}
