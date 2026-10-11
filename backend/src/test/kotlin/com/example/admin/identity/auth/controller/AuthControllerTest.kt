package com.example.admin.identity.auth.controller

import com.example.admin.identity.auth.model.AuthEventType
import com.example.admin.identity.auth.repository.AuthEventRepository
import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserType
import com.example.admin.identity.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val authEventRepository: AuthEventRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
) {

    private val email = "controller-test@example.com"
    private val password = "correct-password"

    @BeforeEach
    fun setUp() {
        cleanup()
        val user = User(email = email, passwordHash = passwordEncoder.encode(password)!!)
        user.userType = UserType.ADMIN
        user.mustChangePassword = false
        userRepository.save(user)
    }

    @AfterEach
    fun tearDown() {
        cleanup()
    }

    private fun cleanup() {
        authEventRepository.deleteAll()
        userRepository.findByEmail(email)?.let { userRepository.delete(it) }
    }

    private fun loginRequest(body: String) = post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.USER_AGENT, "test-agent")
        .content(body)

    private fun json(email: String, password: String) =
        """{"email":"$email","password":"$password"}"""

    @Test
    fun `valid credentials return next step`() {
        mockMvc.perform(loginRequest(json(email, password)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nextStep").value("TOTP_SETUP"))
    }

    @Test
    fun `wrong password returns 401 problem details`() {
        mockMvc.perform(loginRequest(json(email, "wrong-password")))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Invalid email or password"))
    }

    @Test
    fun `unknown email returns the same response as wrong password`() {
        val wrongPassword = mockMvc.perform(loginRequest(json(email, "wrong-password")))
            .andReturn().response
        val unknownEmail = mockMvc.perform(loginRequest(json("nobody@example.com", password)))
            .andReturn().response

        assertEquals(wrongPassword.status, unknownEmail.status)
        assertEquals(wrongPassword.contentAsString, unknownEmail.contentAsString)
    }

    @Test
    fun `blank email returns 400`() {
        mockMvc.perform(loginRequest(json("", password)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `malformed email returns 400`() {
        mockMvc.perform(loginRequest(json("not-an-email", password)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `blank password returns 400`() {
        mockMvc.perform(loginRequest(json(email, "")))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `ip and user agent are stored in auth event`() {
        mockMvc.perform(loginRequest(json(email, password)))
            .andExpect(status().isOk)

        val event = assertNotNull(
            authEventRepository.findAll().firstOrNull { it.eventType == AuthEventType.LOGIN_SUCCESS },
        )
        assertEquals("127.0.0.1", event.ip)
        assertEquals("test-agent", event.userAgent)
    }
}
