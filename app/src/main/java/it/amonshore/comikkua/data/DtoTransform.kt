package it.amonshore.comikkua.data

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.web.AvailableComics

fun AvailableComics.toComics() = Comics(
    id = Comics.NEW_COMICS_ID,
    name = name,
    selected = true,
    publisher = publisher,
    sourceId = sourceId,
    version = version
)