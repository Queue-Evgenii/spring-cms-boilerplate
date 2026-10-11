package com.example.admin.identity.auth.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest (
    @field:NotBlank(message = "Token cannot be blank")
    val stepToken: String,

    @field:NotBlank(message = "Please enter your password")
    val newPassword: String,
) {
    override fun toString() = "ChangePasswordRequest(stepToken=***, newPassword=***)"
}