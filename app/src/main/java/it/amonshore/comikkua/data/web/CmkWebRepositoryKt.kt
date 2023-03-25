package it.amonshore.comikkua.data.web

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class CmkWebRepositoryKt(context: Context) {

    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDaoKt()
    private val _service = CmkWebService.create()

    private suspend fun readAvailableComics(): List<AvailableComics> {
        return _service.getTitles().map {
            AvailableComics().apply {
                name = it
                searchableName = it.uppercase()
            }
        }
    }

    suspend fun refreshAvailableComics(): Int {
        val comics = readAvailableComics()
        _cmkWebDao.refresh(comics)
        return comics.size
    }

    suspend fun getAvailableComicsFlow() = _cmkWebDao.getAvailableComicsFLow()

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