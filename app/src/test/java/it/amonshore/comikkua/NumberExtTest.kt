package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test

class NumberExtTest {

    @Test
    fun parse_to_double() {
        // Arrange
        val text = "10"

        // Act
        val number = parseToDouble(text)

        // Assert
        Assert.assertEquals(10.0, number, 0.0)
    }

    @Test
    fun parse_to_double_with_decimal() {
        // Arrange
        val text = "10.5"

        // Act
        val number = parseToDouble(text)

        // Assert
        Assert.assertEquals(10.5, number, 0.0)
    }

    @Test
    fun parse_to_double_fail() {
        // Arrange
        val text = "a"

        // Act
        val number = parseToDouble(text)

        // Assert
        Assert.assertEquals(0.0, number, 0.0)
    }

    @Test
    fun parse_to_double_null() {
        // Arrange
        val text: String? = null

        // Act
        val number = parseToDouble(text)

        // Assert
        Assert.assertEquals(0.0, number, 0.0)
    }

    @Test
    fun parse_to_string() {
        // Arrange
        val number = 10.5

        // Act
        val text = parseToString(number)

        // Assert
        Assert.assertEquals("10.5", text)
    }

    @Test
    fun parse_interval() {
        // Arrange
        val text = "1,2- 5, 10"
        val expected = listOf(1, 2, 3, 4, 5, 10)

        // Act
        val interval = parseInterval(text)

        // Assert
        Assert.assertEquals(expected, interval)
    }

    @Test
    fun parse_interval_single() {
        // Arrange
        val text = "10"
        val expected = listOf(10)

        // Act
        val interval = parseInterval(text)

        // Assert
        Assert.assertEquals(expected, interval)
    }

    @Test
    fun format_interval() {
        // Arrange
        val list = listOf(1,2,5,6,7)
        val expected = "1-2,5-7"

        // Act
        val interval = list.formatInterval()

        // Assert
        Assert.assertEquals(expected, interval)
    }
}