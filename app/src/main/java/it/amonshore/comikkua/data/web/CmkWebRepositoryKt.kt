package it.amonshore.comikkua.data.web

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService

class CmkWebRepositoryKt(context: Context) {

    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDaoKt()
    private val _service = CmkWebService.create()

    private suspend fun readAvailableComics(): List<AvailableComics> {
        return _service.getTitles().map {
            AvailableComics(
                sourceId = it,
                name = it,
                searchableName = it.uppercase(),
                publisher = "",
                version = 0,
            )
        }
    }

    suspend fun refreshAvailableComics(): Int {
        val comics = readAvailableComics()
        _cmkWebDao.refresh(comics)
        return comics.size
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