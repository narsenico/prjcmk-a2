package it.amonshore.comikkua.workers

import it.amonshore.comikkua.data.comics.ComicsWithReleases
import java.time.LocalDate

class ImportFromOldDatabaseCounter {
    var total: Int = 0
        private set

    var sourced: Int = 0
        private set

    /**
     * La meno recente ultima release tra tutti i comics importati dal vecchio DB.
     * Serve a indicare a CmkWeb da quale mese iniziare il crowling.
     */
    var oldestLastReleaseDate: LocalDate? = null
        private set

    fun count(comics: ComicsWithReleases): ImportFromOldDatabaseCounter {
        if (comics.comics.isSourced) ++sourced
        ++total

        oldestLastReleaseDate = comics.releases.lastOrNull { it.date != null }?.date?.let { date ->
            when {
                oldestLastReleaseDate == null -> date
                date < oldestLastReleaseDate -> date
                else -> oldestLastReleaseDate
            }
        } ?: oldestLastReleaseDate

        return this
    }
}