package com.example.admin.identity.auth.settings

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.auth")
class AuthProperties(
    val maxFailedAttempts: Int = 5,
    val lockDuration: Duration = Duration.ofMinutes(15),
    val totpRequired: Boolean = true,
    val stepTokenTtl: Duration = Duration.ofMinutes(10),
)
