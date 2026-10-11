package com.example.admin.identity.auth.settings

import org.springframework.stereotype.Component

@Component
class PropertiesPasswordPolicySettings(
    private val properties: PasswordPolicyProperties,
) : PasswordPolicySettings {
    override fun minLength() = properties.minLength
    override fun maxLength() = properties.maxLength
    override fun symbolRequired() = properties.requireSymbol
    override fun uppercaseRequired() = properties.requireUppercase
    override fun lowercaseRequired() = properties.requireLowercase
    override fun digitRequired() = properties.requireDigit
}
