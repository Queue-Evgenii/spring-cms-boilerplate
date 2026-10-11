package com.example.admin

import com.example.admin.identity.auth.settings.AuthProperties
import com.example.admin.identity.auth.settings.PasswordPolicyProperties
import com.example.admin.identity.bootstrap.BootstrapAdminProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableConfigurationProperties(
	BootstrapAdminProperties::class,
	AuthProperties::class,
	PasswordPolicyProperties::class,
)
class AdminApplication

fun main(args: Array<String>) {
	runApplication<AdminApplication>(*args)
}
