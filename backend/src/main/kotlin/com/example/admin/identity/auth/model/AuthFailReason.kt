package com.example.admin.identity.auth.model

enum class AuthFailReason {
    UNKNOWN_EMAIL,
    NOT_ALLOWED,
    BLOCKED,
    LOCKED,
    BAD_PASSWORD
}