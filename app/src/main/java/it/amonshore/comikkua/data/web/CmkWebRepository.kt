package it.amonshore.comikkua.data.web

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService
import it.amonshore.comikkua.services.WebComics
import kotlinx.coroutines.yield

class CmkWebRepository(context: Context) {

    private val _cmkWebDao = ComikkuDatabase.getDatabase(context).cmkWebDao()
    private val _service = CmkWebService.create()

    suspend fun deleteAvailableComics() {
        _cmkWebDao.deleteAll()
    }

    suspend fun refreshAvailableComics(): Result<Int> {
        return runCatching {
            _cmkWebDao.deleteAll()

            var count = 0
            var page = 0
            while (true) {
                val result = _service.getAvailableComics(++page)
                if (result.data.isEmpty()) {
                    break
                }

                yield()

                _cmkWebDao.insert(result.data)
                count += result.data.size
            }

            return Result.success(count)
        }
    }

//    suspend fun refreshAvailableComics(): Result<Int> =
//        withContext(Dispatchers.IO + SupervisorJob()) {
//            runCatching {
//                _cmkWebDao.deleteAll()
//
//                var count = 0
//                var page = 0
//                while (isActive) {
//                    val result = _service.getAvailableComics(++page)
//                    if (result.data.isEmpty()) {
//                        break
//                    }
//                    if (!isActive) {
//                        LogHelper.w("Refresh available comics canceled")
//                        break
//                    }
//
//                    _cmkWebDao.insert(result.data)
//                    count += result.data.size
//                }
//
//                return@withContext Result.success(count)
//            }
//        }

    fun getNotFollowedComics() = _cmkWebDao.getNotFollowedComicsFLow()

    suspend fun getAvailableComicsList() = _cmkWebDao.getAvailableComicsList()

    suspend fun findBestAvailableComics(
        name: String,
        publisher: String
    ): Result<List<AvailableComics>> {
        return runCatching {
            return Result.success(
                _service.findComics(name, publisher, limit = 5).map { it.toAvailableComics() })
        }
    }

    private fun WebComics.toAvailableComics() = AvailableComics(
        sourceId = sourceId,
        name = name,
        publisher = publisher,
        version = version,
        lastNumber = null,
        lastReleaseDate = null
    )
}