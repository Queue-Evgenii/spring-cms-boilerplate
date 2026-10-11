package com.example.admin.identity.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_events")
class AuthEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var userId: UUID? = null

    var emailAttempted: String? = null

    @Enumerated(EnumType.STRING)
    var eventType: AuthEventType = AuthEventType.LOGIN_SUCCESS

    @Enumerated(EnumType.STRING)
    var reason: AuthFailReason? = null

    var sessionId: UUID? = null

    var ip: String? = null

    var userAgent: String? = null

    var country: String? = null

    var city: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    var details: Map<String, Any?>? = null

    @CreationTimestamp
    @Column(updatable = false)
    var createdAt: Instant = Instant.now()
}
