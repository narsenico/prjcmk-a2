package it.amonshore.comikkua.data

import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.web.AvailableComics

fun AvailableComics.toComics() = Comics.create(name).apply {
    publisher = this@toComics.publisher
    sourceId = this@toComics.sourceId
    version = this@toComics.version
}