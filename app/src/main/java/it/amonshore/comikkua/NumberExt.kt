package it.amonshore.comikkua

import java.text.NumberFormat
import java.util.Locale

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

fun List<Int>.formatInterval(
    separator: String = ",",
    sequenceSeparator: String = "-"
): String {
    if (isEmpty()) return ""

    val buff = StringBuilder()
    var last = get(0)
    var count = 0
    buff.append(last)
    for (ii in 1 until size) {
        if (get(ii) == last + 1) {
            last = get(ii)
            ++count
        } else {
            if (count > 0) {
                buff.append(sequenceSeparator).append(last)
            }
            last = get(ii)
            count = 0
            buff.append(separator).append(last)
        }
    }
    if (count > 0) {
        buff.append(sequenceSeparator).append(last)
    }

    return buff.toString()
}