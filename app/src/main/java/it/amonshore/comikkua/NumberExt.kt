package it.amonshore.comikkua

import java.text.NumberFormat
import java.util.*

private var numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US)

fun parseToDouble(value: String?) = runCatching {
    if (value.isNullOrEmpty()) {
        0.0
    } else {
        numberFormat.parse(value)?.toDouble() ?: 0.0
    }
}.getOrElse { 0.0 }

fun parseToString(value: Double): String = runCatching {
    numberFormat.format(value)
}.getOrElse { "" }

fun parseInterval(
    text: String,
    separator: String = ",",
    sequenceSeparator: String = "-"
) = text.split(separator).fold(mutableSetOf<Int>()) { acc, token ->
    val range = token.split(sequenceSeparator).map { it.trim().toInt() }
    when (range.size) {
        1 -> {
            acc.add(range[0])
        }
        2 -> {
            acc.addAll((range[0]..range[1]))
        }
    }
    acc
}.toList()