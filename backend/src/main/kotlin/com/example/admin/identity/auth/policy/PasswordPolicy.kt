package com.example.admin.identity.auth.policy

import com.example.admin.identity.auth.settings.PasswordPolicySettings
import org.springframework.stereotype.Component

@Component
class PasswordPolicy(
    private val settings: PasswordPolicySettings,
) {
    fun validate(password: String): List<PasswordViolation> {
        val result = ArrayList<PasswordViolation>()

        if (password.length < settings.minLength()) result.add(PasswordViolation.TOO_SHORT)

        if (password.length > settings.maxLength()) result.add(PasswordViolation.TOO_LONG)

        if (settings.symbolRequired() && password.all { it.isLetterOrDigit() }) result.add(PasswordViolation.NO_SYMBOL)

        if (settings.uppercaseRequired() && !password.any { it.isUpperCase() }) result.add(PasswordViolation.NO_UPPERCASE)

        if (settings.lowercaseRequired() && !password.any { it.isLowerCase() }) result.add(PasswordViolation.NO_LOWERCASE)

        if (settings.digitRequired() && !password.any { it.isDigit() }) result.add(PasswordViolation.NO_DIGIT)

        return result
    }
}
