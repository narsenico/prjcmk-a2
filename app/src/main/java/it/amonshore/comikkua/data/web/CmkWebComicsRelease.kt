package it.amonshore.comikkua.data.web

import com.google.gson.annotations.SerializedName
import it.amonshore.comikkua.data.release.Release

data class CmkWebComicsRelease(
    @SerializedName("release_date") val releaseDate: String,
    val number: Int,
    val title: String,
    val editor: String,
    val last: Boolean,
    val refId: String,
)

fun CmkWebComicsRelease.toRelease(comicsId: Long, tag: String? = null): Release {
    val release = Release()
    release.comicsId = comicsId;
    release.number = number;
    release.date = releaseDate;
    release.tag = tag;
    return release;
}