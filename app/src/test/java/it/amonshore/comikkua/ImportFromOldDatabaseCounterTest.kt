package it.amonshore.comikkua

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.workers.ImportFromOldDatabaseCounter
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

class ImportFromOldDatabaseCounterTest {

    private fun createComicsWithRelease(
        releaseDates: List<LocalDate?>
    ): ComicsWithReleases {
        val comics = Comics(id = 0L, name = "c")
        val releases = releaseDates.withIndex().map { (index, date) ->
            Release(id = index.toLong(), comicsId = comics.id, number = index + 1, date = date)
        }
        return ComicsWithReleases(comics, releases)
    }

    @Test
    fun get_oldest_last_release_date() {
        // Arrange
        val c0 = createComicsWithRelease(listOf(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            LocalDate.of(2023, 3, 1),
        ))
        val c1 = createComicsWithRelease(listOf(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 4, 1),
            null,
        ))
        val counter = ImportFromOldDatabaseCounter()
        val expected = LocalDate.of(2023, 3, 1)

        // Act
        val oldestLastReleaseDate = counter.count(c0).count(c1).oldestLastReleaseDate

        // Assert
        Assert.assertEquals(expected, oldestLastReleaseDate)
    }
}