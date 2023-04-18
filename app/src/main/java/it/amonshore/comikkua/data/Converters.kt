package it.amonshore.comikkua.data

import androidx.room.TypeConverter
import it.amonshore.comikkua.toLocalDate
import it.amonshore.comikkua.toReleaseDate
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun stringToReleaseDate(value: String?): LocalDate? {
        return value?.toLocalDate()
    }

    @TypeConverter
    fun releaseDateToString(value: LocalDate?): String? {
        return value?.toReleaseDate()
    }
}