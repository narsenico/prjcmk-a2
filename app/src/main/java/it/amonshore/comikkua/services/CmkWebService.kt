package it.amonshore.comikkua.services

import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebComicsRelease
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CmkWebService {

    @GET("v1/api/comics/{ref_id}/releases")
    suspend fun getReleases(
        @Path("ref_id") refId: String,
        @Query("numberFrom") numberFrom: Int = 0
    ): List<CmkWebComicsRelease>

    @GET("v1/api/comics")
    suspend fun getAvailableComics(
        @Query("page") page: Int,
        @Query("pageLength") pageLength: Int = 10
    ): GetAvailableComicsResult

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

data class GetAvailableComicsResult(
    val page: Int,
    val pageLength: Int,
    val data: List<AvailableComics>,
)