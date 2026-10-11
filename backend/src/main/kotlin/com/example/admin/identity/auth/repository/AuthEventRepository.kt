package com.example.admin.identity.auth.repository

import com.example.admin.identity.auth.model.AuthEvent
import org.springframework.data.jpa.repository.JpaRepository

interface AuthEventRepository : JpaRepository<AuthEvent, Long>