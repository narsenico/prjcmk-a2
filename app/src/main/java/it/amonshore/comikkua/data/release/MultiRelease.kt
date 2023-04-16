package it.amonshore.comikkua.data.release

import it.amonshore.comikkua.data.comics.Comics

class MultiRelease(
    type: Int,
    comics: Comics,
    release: Release,
    private val otherReleases: List<Release>
) : ComicsRelease(type, comics, release) {
    override val itemType: Int
        get() = ITEM_TYPE

    val size by lazy { otherReleases.size + 1 }

    fun getAllReleases(): Sequence<Release> = sequence {
        yield(release)
        for (other in otherReleases) {
            yield(other)
        }
    }

    fun getAllIds(): Sequence<Long> = getAllReleases().map { it.id }

    fun getAllNumbers(): Sequence<Int> = getAllReleases().map { it.number }

    class Builder(private val comicsRelease: ComicsRelease) {
        private val _otherReleases = mutableListOf<Release>()

        fun add(release: Release) {
            _otherReleases.add(release)
        }

        fun build(): MultiRelease {
            return MultiRelease(0, comicsRelease.comics, comicsRelease.release, _otherReleases)
        }
    }

    companion object {
        const val ITEM_TYPE = 3
    }
}