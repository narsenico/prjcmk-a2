package it.amonshore.comikkua

import com.google.gson.GsonBuilder
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.workers.ComicsWithReleasesDto
import it.amonshore.comikkua.workers.toDto
import it.amonshore.comikkua.workers.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ComicsWithReleasesDtoTest {

    @Test
    fun serialize_comics_with_releases() {
        val cr = ComicsWithReleases(
            comics = Comics(
                id = 1L,
                name = "",
                series = "",
                publisher = "",
                authors = "",
            ),
            releases = listOf(
                Release(
                    id = 1L,
                    comicsId = 1L,
                    number = 1,
                    date = LocalDate.now(),
                    price = 0.0,
                    purchased = false,
                    ordered = false,
                    notes = "",
                    lastUpdate = 0L,
                    removed = false,
                    tag = null
                )
            )
        ).toDto()
        val expected = $$"""
            {"comics":{"id":1,"name":"","series":"","publisher":"","authors":"","price":0.0,"periodicity":null,"reserved":false,"notes":"","lastUpdate":0,"refJsonId":0,"removed":false,"sourceId":null,"selected":false,"version":0},"releases":[{"id":1,"comicsId":1,"number":1,"date":"2026-03-28","price":0.0,"purchased":false,"ordered":false,"notes":"","lastUpdate":0,"removed":false}]}
        """.trimIndent()

        val gson = GsonBuilder()
            .serializeNulls()
            .create()

        val json = gson.toJson(cr)
        assertEquals(expected, json)
    }

    @Test
    fun deserialize_comics_with_releases() {
        val json = $$"""
            {"comics":{"id":1,"name":"","series":"","publisher":"","authors":"","price":0.0,"periodicity":null,"reserved":false,"notes":"","lastUpdate":0,"refJsonId":0,"removed":false,"sourceId":null,"selected":false,"version":0},"releases":[{"id":1,"comicsId":1,"number":1,"date":"2026-03-28","price":0.0,"purchased":false,"ordered":false,"notes":"","lastUpdate":0,"removed":false}]}
        """.trimIndent()
        val expected = ComicsWithReleases(
            comics = Comics(
                id = 1L,
                name = "",
                series = "",
                publisher = "",
                authors = "",
            ),
            releases = listOf(
                Release(
                    id = 1L,
                    comicsId = 1L,
                    number = 1,
                    date = LocalDate.now(),
                    price = 0.0,
                    purchased = false,
                    ordered = false,
                    notes = "",
                    lastUpdate = 0L,
                    removed = false,
                    tag = null
                )
            )
        )

        val gson = GsonBuilder()
            .serializeNulls()
            .create()

        val cr = gson.fromJson(json, ComicsWithReleasesDto::class.java).toEntity()
        assertEquals(expected, cr)
    }
}