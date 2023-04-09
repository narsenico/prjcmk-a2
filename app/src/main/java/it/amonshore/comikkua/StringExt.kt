package it.amonshore.comikkua

import android.net.Uri

infix fun String.containsAll(values: List<String>) =
    values.all { contains(it, true) }

private val SPLIT_REGEX = "\\W+".toRegex()
fun String.splitToWords() = if (isBlank()) emptyList() else split(SPLIT_REGEX)

fun String.uriEncode() = Uri.encode(this) ?: this

fun Array<out String?>.joinToString(separator: String): String {
    if (isEmpty()) {
        return ""
    }

    val buffer = StringBuilder()
    var count = 0
    for (element in this) {
        if (element.isNullOrBlank()) continue
        if (++count > 1) buffer.append(separator)
        buffer.append(element)
    }
    return buffer.toString()
}