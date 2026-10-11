package com.example.admin.identity.user.repository

import com.example.admin.identity.user.model.User
import com.example.admin.identity.user.model.UserStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// @Transactional на тестовом классе: каждый тест выполняется в транзакции,
// которая откатывается в конце. Данные одного теста не видны другому.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val entityManager: EntityManager,
) {

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun newUser(email: String) = User(email = email, passwordHash = "hash")

    @Test
    fun `findByEmail returns saved user`() {
        userRepository.save(newUser("found@example.com"))
        flushAndClear()

        val found = userRepository.findByEmail("found@example.com")

        assertNotNull(found)
        assertEquals("found@example.com", found.email)
    }

    @Test
    fun `findByEmail returns null for unknown email`() {
        assertNull(userRepository.findByEmail("nobody@example.com"))
    }

    @Test
    fun `existsByEmail reflects presence of user`() {
        userRepository.save(newUser("exists@example.com"))
        flushAndClear()

        assertTrue(userRepository.existsByEmail("exists@example.com"))
        assertFalse(userRepository.existsByEmail("missing@example.com"))
    }

    @Test
    fun `new user gets uuid v7 id`() {
        val saved = userRepository.save(newUser("uuid@example.com"))

        val id = assertNotNull(saved.id)
        assertEquals(7, id.version())
    }

    @Test
    fun `new user has expected defaults after reload`() {
        userRepository.save(newUser("defaults@example.com"))
        flushAndClear()

        val user = assertNotNull(userRepository.findByEmail("defaults@example.com"))

        assertEquals(UserStatus.PENDING, user.status)
        assertTrue(user.mustChangePassword)
        assertEquals(0, user.failedLoginAttempts)
        assertNull(user.deletedAt)
    }

    @Test
    fun `soft deleted user is not visible`() {
        val user = userRepository.save(newUser("deleted@example.com"))
        user.deletedAt = Instant.now()
        flushAndClear()

        assertNull(userRepository.findByEmail("deleted@example.com"))
        assertFalse(userRepository.existsByEmail("deleted@example.com"))
    }

    @Test
    fun `duplicate email is rejected by unique index`() {
        userRepository.saveAndFlush(newUser("dup@example.com"))

        assertFailsWith<DataIntegrityViolationException> {
            userRepository.saveAndFlush(newUser("dup@example.com"))
        }
    }

    @Test
    fun `email of soft deleted user can be reused`() {
        val first = userRepository.save(newUser("reuse@example.com"))
        first.deletedAt = Instant.now()
        flushAndClear()

        val second = userRepository.saveAndFlush(newUser("reuse@example.com"))
        flushAndClear()

        val found = assertNotNull(userRepository.findByEmail("reuse@example.com"))
        assertEquals(second.id, found.id)
        assertNotEquals(first.id, found.id)
    }

    @Test
    fun `email with uppercase letters is rejected by check constraint`() {
        assertFailsWith<DataIntegrityViolationException> {
            userRepository.saveAndFlush(newUser("Mixed@Example.com"))
        }
    }
}
