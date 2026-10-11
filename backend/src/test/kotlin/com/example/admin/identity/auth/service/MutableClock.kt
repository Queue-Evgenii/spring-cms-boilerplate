package com.example.admin.identity.auth.service

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(private var current: Instant) : Clock() {

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = current

    fun set(instant: Instant) {
        current = instant
    }

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}
