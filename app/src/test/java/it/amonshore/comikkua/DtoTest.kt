package it.amonshore.comikkua

import it.amonshore.comikkua.data.web.AvailableComics
import org.junit.Assert.*
import org.junit.Test

class DtoTest {

    private fun createAvailableComics(sourceId: String, name: String) = AvailableComics(
        sourceId = sourceId,
        name = name,
        publisher = "",
        version = 0,
    )

    @Test
    fun available_comics_equals_by_source_id() {
        // Arrange
        val a1 = createAvailableComics("A", "aaa");
        val a2 = createAvailableComics("A", "AAAA");
        val b1 = createAvailableComics("B", "aaa");

        // Act
        // Assert
        assertEquals(a1, a2)
        assertNotEquals(a1, b1)
        assertNotEquals(a2, b1)
    }
}