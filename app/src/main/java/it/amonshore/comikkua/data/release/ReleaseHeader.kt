package it.amonshore.comikkua.data.release

import it.amonshore.comikkua.ReleaseTypeDef

data class ReleaseHeader(
    private val relativeId: Long,
    @ReleaseTypeDef val type: Int,
    var totalCount: Int = 0,
    var purchasedCount: Int = 0
) : IReleaseViewModelItem {

    private val _id = BASE_ID + relativeId

    override val id: Long
        get() = _id

    override val itemType: Int
        get() = ITEM_TYPE

    companion object {
        const val ITEM_TYPE = 1
        const val BASE_ID = 90000000L
    }
}