package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test

class StringExtTest {

    @Test
    fun split_to_words() {
        // Arrange
        val source = "A B  C\tD,E, F ;G"
        val expected = listOf("A", "B", "C", "D", "E", "F", "G")

        // Act
        val words = source.splitToWords()

        // Assert
        Assert.assertEquals(expected, words)
    }
}