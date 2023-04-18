package it.amonshore.comikkua.data

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebComicsRelease
import it.amonshore.comikkua.toLocalDate

fun AvailableComics.toComics() = Comics(
    id = Comics.NEW_COMICS_ID,
    name = name,
    selected = true,
    publisher = publisher,
    sourceId = sourceId,
    version = version
)

fun CmkWebComicsRelease.toRelease(comicsId: Long, tag: String? = null): Release {
    return Release(
        id = Release.NEW_RELEASE_ID,
        comicsId = comicsId,
        number = number,
        date = releaseDate.toLocalDate(),
        tag = tag
    )
}