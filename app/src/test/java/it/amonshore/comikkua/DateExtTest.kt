package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class DateExtTest {

    @Test
    fun get_first_day_of_week() {
        // Arrange
        val wednesday = LocalDate.of(2023, 3, 29)
        val expected = LocalDate.of(2023, 3, 27)

        // Act
        val firstDayOfWeek = wednesday.atFirstDayOfWeek()

        // Assert
        Assert.assertEquals(expected, firstDayOfWeek)
    }

    @Test
    fun get_first_day_of_week_already_in() {
        // Arrange
        val monday = LocalDate.of(2023, 3, 27)
        val expected = LocalDate.of(2023, 3, 27)

        // Act
        val firstDayOfWeek = monday.atFirstDayOfWeek()

        // Assert
        Assert.assertEquals(expected, firstDayOfWeek)
    }

    @Test
    fun format_to_year_month_day() {
        // Arrange
        val monday = LocalDate.of(2023, 3, 27)
        val expected = "20230327"

        // Act
        val formatted = monday.toReleaseDate()

        // Assert
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_to_year_month_day_2() {
        // Arrange
        val monday = LocalDate.of(2023, 3, 1)
        val expected = "20230301"

        // Act
        val formatted = monday.toReleaseDate()

        // Assert
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun calc_next_morning() {
        // Arrange
        val start = LocalDateTime.of(2001, 1, 1, 6, 0, 0)
        val time = LocalTime.of(8,0,0)
        val end = LocalDateTime.of(2001, 1, 1, 8, 0, 0)

        // Act
        val calc = start.next(time)

        // Assert
        Assert.assertEquals(end, calc)
    }

    @Test
    fun calc_next_tomorrow_morning() {
        // Arrange
        val start = LocalDateTime.of(2001, 1, 1, 10, 0, 0)
        val time = LocalTime.of(8,0,0)
        val end = LocalDateTime.of(2001, 1, 2, 8, 0, 0)

        // Act
        val calc = start.next(time)

        // Assert
        Assert.assertEquals(end, calc)
    }

    @Test
    fun calc_next_same_time() {
        // Arrange
        val start = LocalDateTime.of(2001, 1, 1, 8, 0, 0)
        val time = LocalTime.of(8,0,0)
        val end = LocalDateTime.of(2001, 1, 1, 8, 0, 0)

        // Act
        val calc = start.next(time)

        // Assert
        Assert.assertEquals(end, calc)
    }

    @Test
    fun weekly_period_from_string() {
        // Arrange
        val count = Random.nextLong(1, 100)
        val str = "W$count"
        val expected = Period.Weekly(count)

        // Act
        val period = Period.from(str)

        // Assert
        Assert.assertEquals(expected, period)
    }

    @Test
    fun monthly_period_from_string() {
        // Arrange
        val count = Random.nextLong(1, 100)
        val str = "M$count"
        val expected = Period.Monthly(count)

        // Act
        val period = Period.from(str)

        // Assert
        Assert.assertEquals(expected, period)
    }

    @Test
    fun yearly_period_from_string() {
        // Arrange
        val count = Random.nextLong(1, 100)
        val str = "Y$count"
        val expected = Period.Yearly(count)

        // Act
        val period = Period.from(str)

        // Assert
        Assert.assertEquals(expected, period)
    }

    @Test
    fun none_period_from_string() {
        // Arrange
        val count = 0
        val str = "Y$count"
        val expected = Period.None

        // Act
        val period = Period.from(str)

        // Assert
        Assert.assertEquals(expected, period)
    }

    @Test
    fun local_date_as_utc() {
        // Arrange
        val localDate = LocalDate.of(2023, Month.APRIL, 16)
        val expected = ZonedDateTime.of(2023, 4, 16, 0, 0, 0, 0, ZoneId.of("UTC"))

        // Act
        val utcDate = localDate.asUtc()

        // Assert
        Assert.assertEquals(expected, utcDate)
    }

    @Test
    fun local_date_as_utc_milliseconds() {
        // Arrange
        val localDate = LocalDate.of(2023, Month.APRIL, 16)
        val expected = 1681603200000L

        // Act
        val utcDate = localDate.asUtcMilliseconds()

        // Assert
        Assert.assertEquals(expected, utcDate)
    }

    @Test
    fun utc_milliseconds_as_local_date() {
        // Arrange
        val millis = 1681603200000L
        val expected = LocalDate.of(2023, Month.APRIL, 16)

        // Act
        val date = millis.asLocalDate()

        // Assert
        Assert.assertEquals(expected, date)
    }

    @Test
    fun available_comics_date_to_local_date() {
        // Arrange
        val source = "2023-02-28T23:00:00.000Z"
        val expected = LocalDate.of(2023, 2,28)

        // Act
        val date = source.fromISO8601Date()?.toLocalDate()

        // Assert
        Assert.assertEquals(expected, date)
    }
}