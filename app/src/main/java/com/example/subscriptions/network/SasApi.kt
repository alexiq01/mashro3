package com.example.subscriptions.network

import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

 data class SasEncryptedRequest(val payload: String)
 data class SasWebResponse(val status: Int = 0, val token: String? = null, val data: JsonObject? = null, val message: String? = null)
 interface SasApi {
    @POST("auth/login") suspend fun login(@Body body: SasEncryptedRequest): Response<SasWebResponse>
    @GET("auth/autoLogin") suspend fun autoLogin(): Response<SasWebResponse>
    @GET("user") suspend fun user(@Header("Authorization") token: String): Response<SasWebResponse>
    @GET("service") suspend fun service(@Header("Authorization") token: String): Response<SasWebResponse>
    @GET("dashboard") suspend fun dashboard(@Header("Authorization") token: String): Response<SasWebResponse>
    @POST("user") suspend fun userAction(@Header("Authorization") token: String, @Body body: SasEncryptedRequest): Response<SasWebResponse>
    @POST("user/extend") suspend fun extend(@Header("Authorization") token: String, @Body body: SasEncryptedRequest): Response<SasWebResponse>
    @POST("service") suspend fun changeService(@Header("Authorization") token: String, @Body body: SasEncryptedRequest): Response<SasWebResponse>
 }
 object ApiFactory {
    const val BASE = "http://admin.skylineiq.com/user/api/index.php/api/"
    val api: SasApi by lazy { val log=HttpLoggingInterceptor().apply{level=HttpLoggingInterceptor.Level.BASIC}; Retrofit.Builder().baseUrl(BASE).client(OkHttpClient.Builder().addInterceptor(log).build()).addConverterFactory(GsonConverterFactory.create()).build().create(SasApi::class.java) }
    fun encoded(json: String) = SasEncryptedRequest(com.example.subscriptions.crypto.SasCrypto.encrypt(json))
    fun plain(json: String) = SasEncryptedRequest(json)
 }
