package com.example.admin.identity.auth.service

import com.example.admin.identity.auth.model.AuthEvent
import com.example.admin.identity.auth.model.AuthEventType
import com.example.admin.identity.auth.model.AuthFailReason
import com.example.admin.identity.auth.repository.AuthEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthEventService(
    private val authEventRepository: AuthEventRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun write(
        type: AuthEventType,
        userId: UUID? = null,
        email: String? = null,
        reason: AuthFailReason? = null,
        sessionId: UUID? = null,
        ip: String? = null,
        userAgent: String? = null,
        details: Map<String, Any?>? = null,
    ) {
        val event = AuthEvent()
        event.eventType = type
        event.userId = userId
        event.emailAttempted = email
        event.reason = reason
        event.sessionId = sessionId
        event.ip = ip
        event.userAgent = userAgent
        event.details = details

        authEventRepository.save(event)
    }
}
