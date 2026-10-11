package com.example.admin.identity.auth.model

enum class NextStep { CHANGE_PASSWORD, TOTP_SETUP, TOTP_REQUIRED, COMPLETE }

sealed class LoginResult {
    data class Success(val nextStep: NextStep, val stepToken: String? = null) : LoginResult()
    data object Failed : LoginResult()
}
