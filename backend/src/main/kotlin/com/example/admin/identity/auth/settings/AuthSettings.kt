package com.example.admin.identity.auth.settings

import java.time.Duration

interface AuthSettings {
    fun maxFailedAttempts(): Int
    fun lockDuration(): Duration
    fun totpRequired(): Boolean
}
