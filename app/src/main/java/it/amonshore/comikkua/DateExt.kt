package it.amonshore.comikkua

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


private val yearMonthDayFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyyMMdd")
}

private val shortFormatter by lazy {
    DateTimeFormatter.ofPattern("EEE dd MMM")
}

private val longFormatter by lazy {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
}

typealias ReleaseDate = String

fun LocalDate.atFirstDayOfWeek(): LocalDate {
    val delta = dayOfWeek.value - 1
    return if (delta > 0) {
        minusDays(delta.toLong())
    } else {
        this
    }
}

fun LocalDate.toReleaseDate(): ReleaseDate = format(yearMonthDayFormatter)

fun ReleaseDate.toLocalDate(): LocalDate = LocalDate.parse(this, yearMonthDayFormatter)

fun LocalDateTime.next(time: LocalTime): LocalDateTime {
    val thisTime = toLocalTime()
    if (thisTime.isAfter(time)) {
        return plusDays(1).with(time)
    }

    return with(time)
}

fun LocalDate.toHumanReadable(
    context: Context,
    formatter: DateTimeFormatter = shortFormatter
): String = when (this) {
    LocalDate.now() -> context.getString(R.string.today)
    LocalDate.now().plusDays(1) -> context.getString(R.string.tomorrow)
    else -> format(formatter)
}

fun LocalDate.toHumanReadableLong(context: Context): String =
    toHumanReadable(context, longFormatter)