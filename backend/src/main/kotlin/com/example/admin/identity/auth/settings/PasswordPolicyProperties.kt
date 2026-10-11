package com.example.admin.identity.auth.settings

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth.password")
class PasswordPolicyProperties(
    val minLength: Int = 8,
    val maxLength: Int = 64,
    val requireSymbol: Boolean = true,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
)
