package com.example.admin.identity.auth.model

import com.example.admin.identity.auth.policy.PasswordViolation

sealed class ChangePasswordResult {
    data class Success(val nextStep: NextStep, val stepToken: String?) : ChangePasswordResult()
    data object InvalidToken : ChangePasswordResult()
    data class WeakPassword(val violations: List<PasswordViolation>) : ChangePasswordResult()
}