package it.amonshore.comikkua.data.web

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService

class CmkWebRepositoryKt(context: Context) {

    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDaoKt()
    private val _service = CmkWebService.create()

    suspend fun refreshAvailableComics(): Int {
        _cmkWebDao.deleteAll()

        var count = 0
        var page = 0
        while (true) {
            val result = _service.getAvailableComics(++page)
            if (result.data.isEmpty()) {
                break
            }

            _cmkWebDao.insert(result.data)
            count += result.data.size
        }

        return count
    }

    fun getAvailableComicsFlow() = _cmkWebDao.getAvailableComicsFLow()

//    /**
//     * Ritorna un [PagingData] con i comics disponibili.
//     *
//     * @param likeName  filtro su [AvailableComics.name] e [AvailableComics.publisher],
//     *                  può comprendere il carattere jolly "%" (Es: "%cybord%")
//     *                  se vuoto, null o "%%" il filtro non viene applicato
//     */
//    fun getAvailableComicsPagingSourceLiveData(likeName: String?): LiveData<PagingData<AvailableComics>> {
//        val factory = if (TextUtils.isEmpty(likeName) || likeName == "%%") {
//            _cmkWebDao.getAvailableComicsPagingSource()
//        } else {
//            _cmkWebDao.getAvailableComicsPagingSource(likeName.orEmpty())
//        }
//        return Pager(
//                config = PagingConfig(
//                        pageSize = PAGE_SIZE,
//                        enablePlaceholders = false
//                ),
//                pagingSourceFactory = {
//                    factory
//                }
//        ).liveData
//    }
}