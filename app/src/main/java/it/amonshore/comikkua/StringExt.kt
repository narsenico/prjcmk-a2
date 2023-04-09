package it.amonshore.comikkua

import android.net.Uri

infix fun String.containsAll(values: List<String>) =
    values.all { contains(it, true) }

private val SPLIT_REGEX = "\\W+".toRegex()
fun String.splitToWords() = if (isBlank()) emptyList() else split(SPLIT_REGEX)

fun String.uriEncode() = Uri.encode(this)