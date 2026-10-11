package com.example.admin

import com.example.admin.identity.bootstrap.BootstrapAdminProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(BootstrapAdminProperties::class)
class AdminApplication

fun main(args: Array<String>) {
	runApplication<AdminApplication>(*args)
}
