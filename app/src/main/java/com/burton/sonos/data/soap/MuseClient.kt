package com.burton.sonos.data.soap

import com.burton.sonos.data.parse.AreaListParser
import com.burton.sonos.domain.NamedGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Singleton
class MuseClient @Inject constructor() {
    private val http: OkHttpClient by lazy { lanHttpsClient() }

    suspend fun listAreas(ip: String, museHouseholdId: String): List<NamedGroup> =
        withContext(Dispatchers.IO) {
            if (ip.isBlank() || museHouseholdId.isBlank()) return@withContext emptyList()
            val url = "https://$ip:1443/api/v1/households/$museHouseholdId/areas"
            val request = Request.Builder()
                .url(url)
                .header("X-Sonos-Api-Key", MUSE_API_KEY)
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext emptyList()
                AreaListParser.parse(body)
            }
        }

    private companion object {
        // Local Muse accepts this well-known client UUID as X-Sonos-Api-Key.
        const val MUSE_API_KEY = "123e4567-e89b-12d3-a456-426655440000"

        fun lanHttpsClient(): OkHttpClient {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val ssl = SSLContext.getInstance("TLS")
            ssl.init(null, arrayOf(trustAll), SecureRandom())
            return OkHttpClient.Builder()
                .sslSocketFactory(ssl.socketFactory, trustAll)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
