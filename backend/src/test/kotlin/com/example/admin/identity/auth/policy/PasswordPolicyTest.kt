package com.example.admin.identity.auth.policy

import com.example.admin.identity.auth.settings.PasswordPolicyProperties
import com.example.admin.identity.auth.settings.PropertiesPasswordPolicySettings
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordPolicyTest {

    private fun policy(properties: PasswordPolicyProperties = PasswordPolicyProperties()) =
        PasswordPolicy(PropertiesPasswordPolicySettings(properties))

    @Test
    fun `valid password has no violations`() {
        assertTrue(policy().validate("Passw0rd!").isEmpty())
    }

    @Test
    fun `short password is rejected`() {
        assertEquals(listOf(PasswordViolation.TOO_SHORT), policy().validate("Pa0!"))
    }

    @Test
    fun `too long password is rejected`() {
        assertEquals(listOf(PasswordViolation.TOO_LONG), policy().validate("Aa1!" + "x".repeat(70)))
    }

    @Test
    fun `password without digit is rejected`() {
        assertEquals(listOf(PasswordViolation.NO_DIGIT), policy().validate("Password!"))
    }

    @Test
    fun `password without uppercase is rejected`() {
        assertEquals(listOf(PasswordViolation.NO_UPPERCASE), policy().validate("passw0rd!"))
    }

    @Test
    fun `password without lowercase is rejected`() {
        assertEquals(listOf(PasswordViolation.NO_LOWERCASE), policy().validate("PASSW0RD!"))
    }

    @Test
    fun `password without symbol is rejected`() {
        assertEquals(listOf(PasswordViolation.NO_SYMBOL), policy().validate("Passw0rdd"))
    }

    @Test
    fun `any non alphanumeric character counts as symbol`() {
        listOf("Passw0rd_", "Passw0rd.", "Passw0rd ", "Passw0rd-", "Passw0rd+").forEach {
            assertTrue(policy().validate(it).isEmpty(), "expected '$it' to be valid")
        }
    }

    @Test
    fun `all violations are reported together`() {
        assertEquals(
            setOf(
                PasswordViolation.TOO_SHORT,
                PasswordViolation.NO_SYMBOL,
                PasswordViolation.NO_UPPERCASE,
                PasswordViolation.NO_LOWERCASE,
                PasswordViolation.NO_DIGIT,
            ),
            policy().validate("").toSet(),
        )
    }

    @Test
    fun `requirements can be switched off`() {
        val relaxed = PasswordPolicyProperties(
            minLength = 1,
            requireSymbol = false,
            requireUppercase = false,
            requireLowercase = false,
            requireDigit = false,
        )

        assertTrue(policy(relaxed).validate("a").isEmpty())
    }

    @Test
    fun `length limits come from settings`() {
        val strict = PasswordPolicyProperties(minLength = 12, maxLength = 16)

        assertEquals(listOf(PasswordViolation.TOO_SHORT), policy(strict).validate("Passw0rd!"))
        assertEquals(listOf(PasswordViolation.TOO_LONG), policy(strict).validate("Passw0rd!" + "x".repeat(10)))
    }
}
