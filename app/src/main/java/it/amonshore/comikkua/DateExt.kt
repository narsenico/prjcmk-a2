package it.amonshore.comikkua

import android.content.Context
import androidx.annotation.IntRange
import java.text.ParseException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

fun LocalDate.asUtc(): ZonedDateTime {
    return LocalDateTime
        .of(this, LocalTime.MIDNIGHT)
        .atZone(ZoneId.of("UTC")) // viene applicata la zona UTC senza variare data/ora
}

fun LocalDate.asUtcMilliseconds(): Long {
    return asUtc().toEpochSecond() * 1000L
}

fun Long.asLocalDate(): LocalDate {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.of("UTC"))
        .toLocalDate()
}

sealed class Period(@IntRange(from = 0) val count: Long) {
    class Weekly(count: Long) : Period(count)
    class Monthly(count: Long) : Period(count)
    class Yearly(count: Long) : Period(count)
    object None : Period(0)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return (other != null &&
                other is Period &&
                other::class == this::class &&
                other.count == count)
    }

    override fun toString(): String {
        return "${this::class.simpleName}($count)"
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    companion object {
        fun from(key: String?): Period {
            if (key.isNullOrEmpty()) return None
            if (key.length < 2) throw ParseException("Cannot parse period", 0)

            val count = key.substring(1).toLong()
            if (count == 0L) return None

            return when (key[0]) {
                'W' -> Weekly(count)
                'M' -> Monthly(count)
                'Y' -> Yearly(count)
                else -> None
            }
        }
    }
}

fun Period.toKey(): String? = when(this) {
    is Period.Weekly -> "W$count"
    is Period.Monthly -> "M$count"
    is Period.Yearly -> "Y$count"
    is Period.None -> null
}

fun LocalDate.plusPeriod(period: Period): LocalDate = when (period) {
    is Period.None -> this
    is Period.Weekly -> plusWeeks(period.count)
    is Period.Monthly -> plusMonths(period.count)
    is Period.Yearly -> plusYears(period.count)
}