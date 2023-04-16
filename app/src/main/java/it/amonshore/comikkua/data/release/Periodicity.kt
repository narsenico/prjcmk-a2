package it.amonshore.comikkua.data.release

import android.content.Context
import it.amonshore.comikkua.Period
import it.amonshore.comikkua.R

data class Periodicity(val period: Period, val label: String) {
    override fun toString(): String = label
}

fun getPeriodicityList(context: Context): List<Periodicity> {
    val keys = context.resources.getStringArray(R.array.comics_periodicity_keys)
    val entries = context.resources.getStringArray(R.array.comics_periodicity_entries)

    return keys.zip(entries).map {
        val period = Period.from((it.first))
        Periodicity(period, it.second)
    }
}