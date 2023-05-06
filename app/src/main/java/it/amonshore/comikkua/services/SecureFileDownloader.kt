package it.amonshore.comikkua.services

import android.content.Context
import it.amonshore.comikkua.R
import it.amonshore.comikkua.ResultEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.net.MalformedURLException

class InsecureUrlException(message: String) : MalformedURLException(message)
class DownloadException(message: String) : Exception(message)

class SecureFileDownloader private constructor(private val secureBaseUrls: List<String>) {

    private val _client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    suspend fun downloadFile(
        url: String,
        contentType: String,
        destination: File
    ): ResultEx<Long, Exception> {
        if (!isSecureUrl(url)) return ResultEx.Failure(InsecureUrlException("Cannot download from insecure url $url"))

        return withContext(Dispatchers.IO + SupervisorJob()) {
            try {
                val httpUrl = url.toHttpUrlOrNull()
                    ?: throw DownloadException("Cannot download: invalid url $url")

                val request = Request.Builder()
                    .url(httpUrl)
                    .header("content-type", contentType)
                    .build()

                val response = _client.newCall(request).execute()
                    .also { it.body ?: throw DownloadException("Cannot download: empty body") }

                val length = when (response.code) {
                    200 -> download(response.body!!, destination)
                    else -> throw DownloadException("Wrong status ${response.code}")
                }

                ResultEx.Success(length)
            } catch (ex: Exception) {
                ResultEx.Failure(ex)
            }
        }
    }

    private fun download(body: ResponseBody, destination: File): Long {
        return destination.outputStream().use {
            body.byteStream().copyTo(it)
        }
    }

    private fun isSecureUrl(url: String): Boolean {
        return secureBaseUrls.any { url.startsWith(it) }
    }

    companion object {
        private var INSTANCE: SecureFileDownloader? = null

        fun getInstance(context: Context): SecureFileDownloader {
            if (INSTANCE == null) {
                val secureBaseUrls =
                    context.resources.getStringArray(R.array.secure_base_urls).toList()
                INSTANCE = SecureFileDownloader(secureBaseUrls)
            }

            return INSTANCE!!
        }
    }
}