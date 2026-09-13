package com.example.subscriptions.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

 data class SasEncryptedRequest(val payload: String)
 data class SasLoginResponse(val token: String? = null, val message: String? = null)
 data class SasUserResponse(val data: JsonObject? = null, val user: JsonObject? = null, val payload: String? = null, val message: String? = null)
 interface SasApi {
    @POST("login") suspend fun login(@Body body: SasEncryptedRequest): Response<SasLoginResponse>
    @POST("index/user") suspend fun getUser(@Header("Authorization") token: String, @Body body: SasEncryptedRequest): Response<SasUserResponse>
 }
 object ApiFactory {
    const val BASE = "http://admin.skylineiq.com/user/api/index.php/api/"
    val api: SasApi by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        Retrofit.Builder().baseUrl(BASE).client(OkHttpClient.Builder().addInterceptor(logger).build()).addConverterFactory(GsonConverterFactory.create()).build().create(SasApi::class.java)
    }
    fun plain(json: String) = SasEncryptedRequest(json)
    fun encoded(json: String) = SasEncryptedRequest(com.example.subscriptions.crypto.SasCrypto.encrypt(json))
}
