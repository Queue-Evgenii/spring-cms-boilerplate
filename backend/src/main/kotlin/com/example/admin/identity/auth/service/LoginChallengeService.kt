package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.NextStep
import com.example.admin.identity.auth.settings.AuthSettings
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoginChallengeService(
    private val redis: StringRedisTemplate,
    private val authSettings: AuthSettings,
) {
    fun issue(userId: UUID, step: NextStep): String {
        val token = TokenHasher.generate()
        redis.opsForValue().set(key(token), "$userId|${step.name}", authSettings.stepTokenTtl())
        return token
    }

    // The token is deleted on read even when the step does not match, so a wrong guess burns it.
    fun consume(token: String, expectedStep: NextStep): UUID? {
        val value = redis.opsForValue().getAndDelete(key(token)) ?: return null
        val (userId, step) = value.split('|', limit = 2)
        if (step != expectedStep.name) return null
        return UUID.fromString(userId)
    }

    private fun key(token: String) = KEY_PREFIX + TokenHasher.hash(token)

    companion object {
        const val KEY_PREFIX = "login-challenge:"
    }
}
