package it.amonshore.comikkua

infix fun String.containsAll(values: List<String>) =
    values.all { contains(it, true) }

private val SPLIT_REGEX = "\\W+".toRegex()
fun String.splitToWords() = split(SPLIT_REGEX)
