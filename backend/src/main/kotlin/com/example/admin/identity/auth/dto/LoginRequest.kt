package com.example.admin.identity.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String,
) {
    override fun toString() = "LoginRequest(email=$email, password=***)"
}
