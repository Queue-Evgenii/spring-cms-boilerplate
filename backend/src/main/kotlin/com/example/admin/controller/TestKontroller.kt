package com.example.admin.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TestController {

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf("status" to "OK", "message" to "Backend is running smoothly!")
    }
}