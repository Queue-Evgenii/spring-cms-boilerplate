package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.AuthEventType
import com.example.admin.identity.auth.model.AuthFailReason
import com.example.admin.identity.auth.model.LoginResult
import com.example.admin.identity.auth.model.NextStep
import com.example.admin.identity.auth.settings.AuthSettings
import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserStatus
import com.example.admin.identity.user.model.UserType
import com.example.admin.identity.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authSettings: AuthSettings,
    private val authEventService: AuthEventService,
    private val clock: Clock,
) {
    private val dummyHash: String = passwordEncoder.encode("dummy-password")!!

    @Transactional
    fun login(email: String, password: String, ip: String?, userAgent: String?): LoginResult {
        val normalizedEmail = email.trim().lowercase()
        val now = Instant.now(clock)

        val user = userRepository.findByEmail(normalizedEmail)
        val passwordMatches = passwordEncoder.matches(password, user?.passwordHash ?: dummyHash)

        if (user == null)
            return fail(AuthFailReason.UNKNOWN_EMAIL, normalizedEmail, null, ip, userAgent)

        if (user.status == UserStatus.BLOCKED)
            return fail(AuthFailReason.BLOCKED, normalizedEmail, user, ip, userAgent)

        if (user.userType != UserType.ADMIN)
            return fail(AuthFailReason.NOT_ALLOWED, normalizedEmail, user, ip, userAgent)

        val lockedUntil = user.lockedUntil
        if (lockedUntil != null && lockedUntil.isAfter(now))
            return fail(
                AuthFailReason.LOCKED, normalizedEmail, user, ip, userAgent,
                details = mapOf("lockedUntil" to lockedUntil.toString()),
            )

        if (!passwordMatches) {
            val attemptNumber = user.failedLoginAttempts + 1
            registerFailedAttempt(user, now, ip, userAgent)
            return fail(
                AuthFailReason.BAD_PASSWORD, normalizedEmail, user, ip, userAgent,
                details = mapOf("failedAttempts" to attemptNumber),
            )
        }

        user.failedLoginAttempts = 0
        user.lockedUntil = null

        val nextStep = nextStep(user)
        authEventService.write(
            type = AuthEventType.LOGIN_SUCCESS,
            userId = user.id,
            email = normalizedEmail,
            ip = ip,
            userAgent = userAgent,
            details = mapOf("stage" to "PASSWORD", "nextStep" to nextStep.name),
        )
        return LoginResult.Success(nextStep)
    }

    private fun registerFailedAttempt(user: User, now: Instant, ip: String?, userAgent: String?) {
        user.failedLoginAttempts += 1
        if (user.failedLoginAttempts < authSettings.maxFailedAttempts()) return

        val until = now.plus(authSettings.lockDuration())
        user.lockedUntil = until
        user.failedLoginAttempts = 0

        authEventService.write(
            type = AuthEventType.ACCOUNT_LOCKED,
            userId = user.id,
            email = user.email,
            ip = ip,
            userAgent = userAgent,
            details = mapOf("lockedUntil" to until.toString()),
        )
    }

    private fun nextStep(user: User): NextStep = when {
        user.mustChangePassword -> NextStep.CHANGE_PASSWORD
        user.totpEnabledAt != null -> NextStep.TOTP_REQUIRED
        authSettings.totpRequired() -> NextStep.TOTP_SETUP
        else -> NextStep.COMPLETE
    }

    private fun fail(
        reason: AuthFailReason,
        email: String,
        user: User?,
        ip: String?,
        userAgent: String?,
        details: Map<String, Any?>? = null,
    ): LoginResult {
        authEventService.write(
            type = AuthEventType.LOGIN_FAILED,
            userId = user?.id,
            email = email,
            reason = reason,
            ip = ip,
            userAgent = userAgent,
            details = details,
        )
        return LoginResult.Failed
    }
}
