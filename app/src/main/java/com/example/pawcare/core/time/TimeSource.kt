package com.example.pawcare.core.time

import java.time.Clock
import java.time.ZoneId
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

fun interface ZoneSource { fun current(): ZoneId }

@Singleton
class TimeSource @Inject constructor(val clock: Clock, private val zones: ZoneSource) {
    fun zone(): ZoneId = zones.current()
    fun today(): LocalDate = clock.instant().atZone(zone()).toLocalDate()
}
