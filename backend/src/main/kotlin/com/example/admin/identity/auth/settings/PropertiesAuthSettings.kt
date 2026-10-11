package com.example.admin.identity.auth.settings

import org.springframework.stereotype.Component

@Component
class PropertiesAuthSettings(private val properties: AuthProperties) : AuthSettings {
    override fun maxFailedAttempts() = properties.maxFailedAttempts
    override fun lockDuration() = properties.lockDuration
    override fun totpRequired() = properties.totpRequired
}
