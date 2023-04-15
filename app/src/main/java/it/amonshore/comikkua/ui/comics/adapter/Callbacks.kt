package it.amonshore.comikkua.ui.comics.adapter

import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.web.AvailableComics

typealias OnComicsClick = (comics: ComicsWithReleases) -> Unit
typealias OnComicsMenuClick = (comics: ComicsWithReleases) -> Unit
typealias OnAvailableComicsFollow = (comics: AvailableComics) -> Unit
typealias OnAvailableComicsMenuClick = (comics: AvailableComics) -> Unit