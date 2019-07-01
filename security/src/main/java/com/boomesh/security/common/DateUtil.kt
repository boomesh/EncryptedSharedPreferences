package com.boomesh.security.common

import java.util.*

internal class DateUtil(private val calendarFactory: (() -> Calendar)) {
    val now: Calendar
        get() {
            // returns a new instance (not a singleton)
            return calendarFactory()
        }

    fun addYearsTo(calendar: Calendar, years: Int): Date {
        calendar.add(Calendar.YEAR, years)
        return calendar.time
    }
}