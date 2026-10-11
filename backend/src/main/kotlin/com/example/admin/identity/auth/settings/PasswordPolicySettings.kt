package com.example.admin.identity.auth.settings

interface PasswordPolicySettings {
    fun minLength(): Int
    fun maxLength(): Int
    fun symbolRequired(): Boolean
    fun uppercaseRequired(): Boolean
    fun lowercaseRequired(): Boolean
    fun digitRequired(): Boolean
}
