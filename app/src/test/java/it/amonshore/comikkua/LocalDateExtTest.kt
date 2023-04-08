package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LocalDateExtTest {

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
        val formatted = monday.toYearMonthDay()

        // Assert
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_to_year_month_day_2() {
        // Arrange
        val monday = LocalDate.of(2023, 3, 1)
        val expected = "20230301"

        // Act
        val formatted = monday.toYearMonthDay()

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

        //
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

        //
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

        //
        Assert.assertEquals(end, calc)
    }
}