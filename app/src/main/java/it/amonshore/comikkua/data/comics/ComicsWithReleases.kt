package it.amonshore.comikkua.data.comics

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Relation
import it.amonshore.comikkua.data.release.Release

@Entity
data class ComicsWithReleases(
    @Embedded val comics: Comics,
    @Relation(
        entity = Release::class,
        parentColumn = "id",
        entityColumn = "comicsId"
    ) val releases: List<Release>
) {

    @delegate:Ignore
    val lastPurchasedRelease: Release? by lazy {
        if (releases.isEmpty()) {
            null
        } else {
            releases.lastOrNull { it.purchased }
        }
    }

    @delegate:Ignore
    val nextToPurchaseRelease: Release? by lazy {
        if (releases.isEmpty()) {
            null
        } else {
            releases.firstOrNull { !it.purchased }
        }
    }

    @delegate:Ignore
    val lastRelease: Release? by lazy {
        if (releases.isEmpty()) {
            null
        } else {
            releases.last()
        }
    }

    @delegate:Ignore
    val notPurchasedReleaseCount: Int by lazy {
        if (releases.isEmpty()) {
            0
        } else {
            releases.count { !it.purchased }
        }
    }

    @delegate:Ignore
    val nextReleaseNumber: Int by lazy {
        lastRelease?.let { it.number + 1 } ?: 0
    }

    companion object {
        fun createNew() = ComicsWithReleases(
            comics = Comics(id = Comics.NEW_COMICS_ID, name = "", selected = true),
            releases = emptyList()
        )
    }
}