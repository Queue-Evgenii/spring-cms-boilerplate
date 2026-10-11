package com.example.admin.identity.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
class User(
    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,
) {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    var firstName: String? = null
    var lastName: String? = null
    var middleName: String? = null
    var phone: String? = null

    var mustChangePassword: Boolean = true
    var passwordChangedAt: Instant? = null

    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.PENDING

    @Enumerated(EnumType.STRING)
    var userType: UserType = UserType.CUSTOMER

    var failedLoginAttempts: Int = 0
    var lockedUntil: Instant? = null

    var totpSecretEncrypted: String? = null
    var totpEnabledAt: Instant? = null
    var totpLastUsedStep: Long? = null

    var lastLoginAt: Instant? = null
    var deletedAt: Instant? = null

    @CreationTimestamp
    @Column(updatable = false)
    var createdAt: Instant = Instant.now()

    @UpdateTimestamp
    var updatedAt: Instant = Instant.now()
}
