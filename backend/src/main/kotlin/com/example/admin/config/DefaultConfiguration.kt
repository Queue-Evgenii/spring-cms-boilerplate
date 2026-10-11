package com.example.admin.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class DefaultConfiguration {
    @Bean fun clock(): Clock = Clock.systemUTC()
}