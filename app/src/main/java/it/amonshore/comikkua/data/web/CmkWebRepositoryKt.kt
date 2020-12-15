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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class CmkWebRepositoryKt(context: Context) {

    companion object {
        @JvmStatic
        val FIREBASE_PROJECT_ID = Firebase.firestore.app.options.projectId

//        private const val PAGE_SIZE = 10
    }

    private val _firestore = Firebase.firestore;
    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDaoKt()

    /**
     * Aggiorna il repository locale.
     */
    private fun refreshAvailableComics(vararg availableComics: AvailableComics) = _cmkWebDao.refresh(*availableComics);

    /**
     * Legge e ritorna i comics dalla rete.
     *
     * @return  lista di comics
     */
    private suspend fun readAvailableComics(): List<AvailableComics> {
        // leggo tutti i comics così come sono
        val data = _firestore.collection("comics")
                .get()
                .await()

        // e li mappo in oggetti AvailableComics
        // il campo "id" non è considerato durante la conversione, devo impostarlo a mano
        return data.map { doc -> doc.toObject<AvailableComics>().withSourceId(doc.id) }
    }

    /**
     * Legge i comics dalla rete e aggiorna il repository locale.
     *
     * @return numero di comics disponibili
     */
    suspend fun refreshAvailableComics(): Int {
        val comics = readAvailableComics()
        refreshAvailableComics(*comics.toTypedArray())
        return comics.size
    }

    /**
     * Ritorna il flusso di tutti i comics disponibili.
     */
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