package com.example.admin.identity.bootstrap

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "app.bootstrap-admin")
data class BootstrapAdminProperties (
    @NotBlank
    val email: String,

    @NotBlank
    val password: String
) {
    override fun toString() = "BootstrapAdminProperties(email=$email, password=***)"
}