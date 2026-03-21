package it.amonshore.comikkua.services

import com.google.gson.annotations.SerializedName

data class WebComics(
    @SerializedName("ref_id") val sourceId: String,
    @SerializedName("title") val name: String,
    @SerializedName("editor") val publisher: String,
    @SerializedName("reprint") val version: Int
)