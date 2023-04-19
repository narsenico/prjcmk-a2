package it.amonshore.comikkua.data.web

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class CmkWebRepository(context: Context) {

    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDao()
    private val _service = CmkWebService.create()

    suspend fun deleteAvailableComics() {
        _cmkWebDao.deleteAll()
    }

    suspend fun refreshAvailableComics(): Result<Int> =
        withContext(Dispatchers.IO + SupervisorJob()) {
            runCatching {
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

                return@withContext Result.success(count)
            }
        }

    fun getNotFollowedComics() = _cmkWebDao.getNotFollowedComicsFLow()

    suspend fun getAvailableComicsList() = _cmkWebDao.getAvailableComicsList()
}