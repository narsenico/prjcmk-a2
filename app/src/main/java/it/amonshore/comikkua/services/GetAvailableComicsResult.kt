package it.amonshore.comikkua.services

import it.amonshore.comikkua.data.web.AvailableComics

data class GetAvailableComicsResult(
    val page: Int,
    val pageLength: Int,
    val data: List<AvailableComics>,
)
