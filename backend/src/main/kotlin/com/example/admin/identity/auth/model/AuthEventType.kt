package com.example.admin.identity.auth.model

enum class AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    TOTP_FAILED,
    TOTP_SUCCESS,
    LOGOUT,
    SESSION_KICKED,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED
}