package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.NextStep
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(properties = ["app.auth.step-token-ttl=5m"])
@ActiveProfiles("test")
class LoginChallengeServiceTest(
    @Autowired private val service: LoginChallengeService,
    @Autowired private val redis: StringRedisTemplate,
) {

    @Test
    fun `consume returns the user id of the issued token`() {
        val userId = UUID.randomUUID()
        val token = service.issue(userId, NextStep.TOTP_REQUIRED)

        assertEquals(userId, service.consume(token, NextStep.TOTP_REQUIRED))
    }

    @Test
    fun `token can be used only once`() {
        val token = service.issue(UUID.randomUUID(), NextStep.TOTP_REQUIRED)

        assertNotNull(service.consume(token, NextStep.TOTP_REQUIRED))
        assertNull(service.consume(token, NextStep.TOTP_REQUIRED))
    }

    @Test
    fun `token is rejected for another step and is burned`() {
        val token = service.issue(UUID.randomUUID(), NextStep.TOTP_REQUIRED)

        assertNull(service.consume(token, NextStep.CHANGE_PASSWORD))
        assertNull(service.consume(token, NextStep.TOTP_REQUIRED))
    }

    @Test
    fun `unknown token is rejected`() {
        assertNull(service.consume("not-a-real-token", NextStep.TOTP_REQUIRED))
    }

    @Test
    fun `tokens are unique`() {
        val userId = UUID.randomUUID()

        assertNotEquals(
            service.issue(userId, NextStep.TOTP_REQUIRED),
            service.issue(userId, NextStep.TOTP_REQUIRED),
        )
    }

    @Test
    fun `raw token is not stored and key has expiration`() {
        val token = service.issue(UUID.randomUUID(), NextStep.TOTP_REQUIRED)

        assertNotEquals(true, redis.hasKey(LoginChallengeService.KEY_PREFIX + token))
        val ttl = assertNotNull(
            redis.getExpire(LoginChallengeService.KEY_PREFIX + TokenHasher.hash(token), TimeUnit.SECONDS),
        )
        assertTrue(ttl in 1L..300L)
    }
}
