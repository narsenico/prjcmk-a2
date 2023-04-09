package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test

class StringExtTest {

    @Test
    fun `split string to words`() {
        // Arrange
        val source = "A B  C\tD,E, F ;G"
        val expected = listOf("A", "B", "C", "D", "E", "F", "G")

        // Act
        val words = source.splitToWords()

        // Assert
        Assert.assertEquals(expected, words)
    }

    @Test
    fun `join strings`() {
        // Arrange
        val source = arrayOf("1", null, "2", "", "3", "4")
        val expected = "1-2-3-4"

        // Act
        val join = source.joinToString("-")

        // Assert
        Assert.assertEquals(expected, join)
    }

    @Test
    fun `join empty strings`() {
        // Arrange
        val source = emptyArray<String>()
        val expected = ""

        // Act
        val join = source.joinToString("-")

        // Assert
        Assert.assertEquals(expected, join)
    }
}