package com.example.admin.identity.auth.policy

enum class PasswordViolation {
    TOO_SHORT,
    TOO_LONG,
    NO_DIGIT,
    NO_UPPERCASE,
    NO_LOWERCASE,
    NO_SYMBOL
}