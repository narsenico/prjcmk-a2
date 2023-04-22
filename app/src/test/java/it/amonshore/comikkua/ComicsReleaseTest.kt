package it.amonshore.comikkua

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.ComicsReleaseJoinType
import it.amonshore.comikkua.data.release.DatedRelease
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.data.release.LostRelease
import it.amonshore.comikkua.data.release.MissingRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.ReleaseHeader
import it.amonshore.comikkua.data.release.toReleaseViewModelItems
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ComicsReleaseTest {

    private fun createComicsReleases(
        comicsId: Long,
        name: String,
        releaseType: Int,
        number: Int,
        date: LocalDate?,
        purchased: Boolean,
        ordered: Boolean
    ): ComicsRelease {
        val comics = Comics(id = comicsId, name = name)
        val release = Release(
            id = number.toLong(),
            comicsId = comicsId,
            number = number,
            date = date,
            purchased = purchased,
            ordered = ordered,
        )

        return ComicsRelease(releaseType, comics, release)
    }

    private fun produceComicsReleases(): List<ComicsRelease> {
        return listOf(
            createComicsReleases(
                comicsId = 1L,
                name = "c1",
                releaseType = LostRelease.TYPE,
                number = 0,
                date = LocalDate.of(2021, 6, 14),
                purchased = false,
                ordered = false,
            ),
            createComicsReleases(
                comicsId = 2L,
                name = "c2",
                releaseType = DatedRelease.TYPE,
                number = 9,
                date = LocalDate.now(),
                purchased = false,
                ordered = false,
            ),
            createComicsReleases(
                comicsId = 3L,
                name = "c3",
                releaseType = DatedRelease.TYPE_NEXT,
                number = 8,
                date = LocalDate.now().plusWeeks(1),
                purchased = false,
                ordered = false,
            ),
            createComicsReleases(
                comicsId = 4L,
                name = "c4",
                releaseType = DatedRelease.TYPE_OTHER,
                number = 7,
                date = LocalDate.now().plusWeeks(2),
                purchased = false,
                ordered = false,
            ),
            createComicsReleases(
                comicsId = 5L,
                name = "c5",
                releaseType = MissingRelease.TYPE,
                number = 1,
                date = null,
                purchased = false,
                ordered = false,
            ),
            createComicsReleases(
                comicsId = 5L,
                name = "c5",
                releaseType = MissingRelease.TYPE,
                number = 2,
                date = null,
                purchased = false,
                ordered = false,
            )
        )
    }

    private fun List<ComicsRelease>.toMulti(): MultiRelease {
        val builder = MultiRelease.Builder(get(0))
        for (r in 1 until size) {
            builder.add(get(r).release)
        }
        return builder.build()
    }

    private fun produceReleaseViewModelItemsWithMissingReleasesJoin(): List<IReleaseViewModelItem> {
        val crs = produceComicsReleases()
        return listOf(
            ReleaseHeader(1, LostRelease.TYPE, 1, 0),
            crs[0],
            ReleaseHeader(2, DatedRelease.TYPE, 1, 0),
            crs[1],
            ReleaseHeader(3, DatedRelease.TYPE_NEXT, 1, 0),
            crs[2],
            ReleaseHeader(4, DatedRelease.TYPE_OTHER, 1, 0),
            crs[3],
            ReleaseHeader(5, MissingRelease.TYPE, 2, 0),
            crs.subList(4, 5).toMulti(),
        )
    }

    @Test
    fun toReleaseViewModelItems_joining_MissingReleases() {
        // Arrange
        val list = produceComicsReleases()
        val expected = produceReleaseViewModelItemsWithMissingReleasesJoin()

        // Act
        val items = list.toReleaseViewModelItems(ComicsReleaseJoinType.MissingReleases)

        // Assert
        assertEquals(expected, items)
    }
}