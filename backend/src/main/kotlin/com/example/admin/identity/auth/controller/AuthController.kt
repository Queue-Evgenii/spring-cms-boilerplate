package com.example.admin.identity.auth.controller

import com.example.admin.identity.auth.dto.ChangePasswordRequest
import com.example.admin.identity.auth.dto.LoginRequest
import com.example.admin.identity.auth.dto.LoginResponse
import com.example.admin.identity.auth.model.ChangePasswordResult
import com.example.admin.identity.auth.model.LoginResult
import com.example.admin.identity.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val result = authService.login(
            email = request.email,
            password = request.password,
            ip = httpRequest.remoteAddr,
            userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT),
        )

        return when (result) {
            is LoginResult.Success -> ResponseEntity.ok(LoginResponse(result.nextStep, result.stepToken))
            LoginResult.Failed -> ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password"))
        }
    }

    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val result = authService.changePassword(
            stepToken = request.stepToken,
            password = request.newPassword,
            ip = httpRequest.remoteAddr,
            userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT),
        )

        return when (result) {
            is ChangePasswordResult.Success -> ResponseEntity.ok(LoginResponse(result.nextStep, result.stepToken))
            ChangePasswordResult.InvalidToken -> ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or expired token"))
            is ChangePasswordResult.WeakPassword -> {
                val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Password does not meet requirements")
                problem.setProperty("violations", result.violations)
                ResponseEntity.badRequest().body(problem)
            }
        }
    }
}
