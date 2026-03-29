package it.amonshore.comikkua.data.web

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.services.CmkWebService
import it.amonshore.comikkua.services.WebComics
import kotlinx.coroutines.yield
import okhttp3.MediaType
import retrofit2.HttpException
import kotlin.io.path.createTempFile

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

    fun getNotFollowedComics() = _cmkWebDao.getNotFollowedComicsFLow()

    suspend fun getAvailableComicsList() = _cmkWebDao.getAvailableComicsList()

    // TODO: non va bene che sia nel repository, il chiamante deve usare direttamente il servizio
    //  a meno che findBestAvailableComics non aggiorni anche il DB
    suspend fun findBestAvailableComics(
        name: String
    ): Result<List<AvailableComics>> {
        return runCatching {
            return Result.success(
                _service.findComicsByFuzzy(name, minScore = 0.2F, limit = 5).map { it.toAvailableComics() })
        }
    }

    // TODO: non va bene che sia nel repository, il chiamante deve usare direttamente il servizio
    suspend fun downloadComicsImageBySourceId(sourceId: String): DownloadComicsImageResult {
        return runCatching {
            val response = _service.downloadImageComics(sourceId)
            if (response.isSuccessful) {
                val body = response.body()!!
                val file = createTempFile(sourceId, body.contentType().toExtension()).toFile()
                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                DownloadComicsImageResult.Success(file.toUri())
            } else if (response.code() == 404) {
                DownloadComicsImageResult.NotFound
            } else {
                DownloadComicsImageResult.Error(HttpException(response))
            }
        }.getOrElse { err -> DownloadComicsImageResult.Error(err) }
    }

    private fun MediaType?.toExtension() = this?.let {
        MimeTypeMap.getSingleton().getExtensionFromMimeType("${type}/${subtype}")
    } ?: "jpg"

    private fun WebComics.toAvailableComics() = AvailableComics(
        sourceId = sourceId,
        name = name,
        publisher = publisher,
        version = version,
        lastNumber = null,
        lastReleaseDate = null
    )
}