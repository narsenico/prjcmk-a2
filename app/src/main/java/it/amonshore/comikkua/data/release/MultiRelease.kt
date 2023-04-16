package it.amonshore.comikkua.data.release

class MultiRelease(private val otherReleases: List<Release>) : ComicsRelease() {
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
            return MultiRelease(_otherReleases).apply {
                comics = comicsRelease.comics
                release = comicsRelease.release
            }
        }
    }

    companion object {
        const val ITEM_TYPE = 3
    }
}