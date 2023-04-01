package it.amonshore.comikkua

import org.junit.Assert
import org.junit.Test

class ResultExTest {

    @Test
    fun success() {
        // Arrange
        val source = ResultEx.Success(1)

        // Act
        val value = source.getOrNull()

        // Assert
        Assert.assertEquals(1, value)
    }

    @Test
    fun failure() {
        // Arrange
        val source = "err".toFailure<Int, String>()

        // Act
        val value = source.getOrNull()

        // Assert
        Assert.assertNull(value)
    }

    @Test
    fun map_on_success() {
        // Arrange
        val source = ResultEx.Success(1)

        // Act
        val value = source
            .map { it + 1 }
            .map { it * 2 }
            .getOrNull()

        // Assert
        Assert.assertEquals(4, value)
    }

    @Test
    fun map_on_failure() {
        // Arrange
        val source = "err".toFailure<Int, String>()

        // Act
        val value = source
            .map { it + 1 }
            .map { it * 2 }
            .getOrNull()

        // Assert
        Assert.assertNull(value)
    }

    @Test
    fun flat_map_on_failure() {
        // Arrange
        val source = 1.toSuccess<Int, String>()

        // Act
        val value = source
            .flatMap { "err".toFailure<String, String>() }
            .map { it.uppercase() }
            .recover { it }
            .getOrNull()

        // Assert
        Assert.assertEquals("err", value)
    }
}