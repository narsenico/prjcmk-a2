package it.amonshore.comikkua.workers

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ComicsDto(
    val id: Long,
    val name: String,
    val series: String = "",
    val publisher: String = "",
    val authors: String = "",
    val price: Double = 0.0,
    val periodicity: String? = null,
    val reserved: Boolean = false,
    val notes: String = "",
    val lastUpdate: Long = 0,
    val refJsonId: Long = 0,
    val removed: Boolean = false,
    val sourceId: String? = null,
    val selected: Boolean = false,
    val version: Int = 0
)

data class ReleaseDto(
    val id: Long,
    val comicsId: Long,
    val number: Int,
    val date: String? = null,
    val price: Double = 0.0,
    val purchased: Boolean = false,
    val ordered: Boolean = false,
    val notes: String? = null,
    val lastUpdate: Long = 0,
    val removed: Boolean = false,
)

data class ComicsWithReleasesDto(
    val comics: ComicsDto,
    val releases: List<ReleaseDto>
)

fun Comics.toDto() = ComicsDto(
    id = id,
    name = name,
    series = series,
    publisher = publisher,
    authors = authors,
    price = price,
    periodicity = periodicity,
    reserved = reserved,
    notes = notes,
    lastUpdate = lastUpdate,
    refJsonId = refJsonId,
    removed = removed,
    sourceId = sourceId,
    selected = selected,
    version = version,
)

fun Release.toDto() = ReleaseDto(
    id = id,
    comicsId = comicsId,
    number = number,
    date = date?.format(DateTimeFormatter.ISO_DATE),
    price = price,
    purchased = purchased,
    ordered = ordered,
    notes = notes,
    lastUpdate = lastUpdate,
    removed = removed,
)

fun ComicsWithReleases.toDto() = ComicsWithReleasesDto(
    comics = comics.toDto(),
    releases = releases.map { it.toDto() }
)

fun ComicsDto.toEntity() = Comics(
    id = id,
    name = name,
    series = series,
    publisher = publisher,
    authors = authors,
    price = price,
    periodicity = periodicity,
    reserved = reserved,
    notes = notes,
    lastUpdate = lastUpdate,
    refJsonId = refJsonId,
    removed = removed,
    sourceId = sourceId,
    selected = selected,
    version = version,
)

fun ReleaseDto.toEntity() = Release(
    id = id,
    comicsId = comicsId,
    number = number,
    date = date?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) },
    price = price,
    purchased = purchased,
    ordered = ordered,
    notes = notes,
    lastUpdate = lastUpdate,
    removed = removed
)

fun ComicsWithReleasesDto.toEntity() = ComicsWithReleases(
    comics = comics.toEntity(),
    releases = releases.map { it.toEntity() }
)