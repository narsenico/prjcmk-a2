package it.amonshore.comikkua

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.atFirstDayOfWeek(): LocalDate {
    val delta = dayOfWeek.value - 1
    return if (delta > 0) {
        minusDays(delta.toLong())
    } else {
        this
    }
}

private val yearMonthDayFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyyMMdd")
}

fun LocalDate.toYearMonthDay(): String = format(yearMonthDayFormatter)