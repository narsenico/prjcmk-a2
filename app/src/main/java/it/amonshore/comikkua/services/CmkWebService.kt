package it.amonshore.comikkua.services

import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.data.web.CmkWebComicsRelease
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CmkWebService {

    @GET("v1/api/title/{title}/releases")
    suspend fun getReleasesByTitle(
        @Path("title") title: String,
        @Query("numberFrom") numberFrom: Int = 0
    ): List<CmkWebComicsRelease>

    @GET("v1/api/comics?mode=array-of-title")
    suspend fun getTitles(): List<String>

    companion object {
        fun create(): CmkWebService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.CMKWEB_SERVICE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CmkWebService::class.java)
        }
    }
}