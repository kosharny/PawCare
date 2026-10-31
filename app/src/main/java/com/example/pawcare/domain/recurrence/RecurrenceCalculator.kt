package com.example.pawcare.domain.recurrence

import com.example.pawcare.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object RecurrenceCalculator {
    fun dateAt(anchor: LocalDate, interval: Int, unit: IntervalUnit, index: Long): LocalDate {
        require(interval > 0 && index >= 0)
        val amount = Math.multiplyExact(interval.toLong(), index)
        return when (unit) {
            IntervalUnit.DAYS -> anchor.plusDays(amount)
            IntervalUnit.WEEKS -> anchor.plusWeeks(amount)
            IntervalUnit.MONTHS -> anchor.plusMonths(amount)
        }
    }

    fun fixedDates(plan: CarePlan, from: LocalDate, through: LocalDate): List<LocalDate> {
        require(!through.isBefore(from) && plan.interval > 0)
        val units = when (plan.unit) {
            IntervalUnit.DAYS -> ChronoUnit.DAYS.between(plan.anchorDate, from)
            IntervalUnit.WEEKS -> ChronoUnit.WEEKS.between(plan.anchorDate, from)
            IntervalUnit.MONTHS -> ChronoUnit.MONTHS.between(plan.anchorDate, from)
        }
        var index = (units / plan.interval - 1).coerceAtLeast(0)
        while (dateAt(plan.anchorDate, plan.interval, plan.unit, index) < from) index++
        val dates = mutableListOf<LocalDate>()
        do {
            val date = dateAt(plan.anchorDate, plan.interval, plan.unit, index++)
            if (date > through && dates.isNotEmpty()) break
            dates += date
        } while (dates.last() <= through && dates.size < 400)
        return dates
    }

    fun overdue(occurrence: CareOccurrence, now: Instant, zone: ZoneId): Boolean {
        if (occurrence.status != OccurrenceStatus.PENDING) return false
        val local = now.atZone(zone)
        return occurrence.time?.let { occurrence.date.atTime(it) < local.toLocalDateTime() }
            ?: (occurrence.date < local.toLocalDate())
    }
}
