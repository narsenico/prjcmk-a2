package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

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
}