package com.example.subscriptions.repository

import com.example.subscriptions.crypto.SasCrypto
import com.example.subscriptions.model.Subscription
import com.example.subscriptions.network.ApiFactory
import com.example.subscriptions.storage.SecurePrefs
import com.example.subscriptions.util.JsonUtils
import com.google.gson.Gson
import com.google.gson.JsonObject

class SubscriptionRepository(private val prefs: SecurePrefs) {
    suspend fun login(username: String, password: String): Result<String> = runCatching {
        val json=Gson().toJson(mapOf("username" to username,"password" to password))
        var response=ApiFactory.api.login(ApiFactory.encoded(json)); if(!response.isSuccessful) response=ApiFactory.api.login(ApiFactory.plain(json))
        val token=response.body()?.token ?: throw IllegalStateException("اسم المستخدم أو كلمة المرور غير صحيحة")
        prefs.token=token; prefs.username=username; prefs.password=password; token
    }
    suspend fun fetch(token: String): Result<Subscription> = runCatching {
        val json=Gson().toJson(mapOf("username" to prefs.username.orEmpty()))
        var response=ApiFactory.api.getUser("Bearer $token",ApiFactory.encoded(json)); if(response.code()==401) throw UnauthorizedException(); if(!response.isSuccessful) response=ApiFactory.api.getUser("Bearer $token",ApiFactory.plain(json)); if(!response.isSuccessful) throw IllegalStateException("تعذر تحميل بيانات الاشتراك")
        val body=response.body() ?: throw IllegalStateException("تعذر تحميل بيانات الاشتراك"); val raw=body.payload?.let { runCatching { SasCrypto.decrypt(it) }.getOrNull() }; val root=if(raw!=null) Gson().fromJson(raw,JsonObject::class.java) else Gson().fromJson(Gson().toJson(body),JsonObject::class.java); val result=JsonUtils.subscription(root); prefs.expiry=result.expiryMillis; result
    }
    suspend fun refreshOrRelogin(): Result<Subscription> { val current=prefs.token; if(current!=null){ val cached=fetch(current); if(cached.isSuccess)return cached }; val u=prefs.username; val p=prefs.password; if(u.isNullOrBlank()||p.isNullOrBlank())return Result.failure(IllegalStateException("لم يتم تسجيل الدخول بعد")); return login(u,p).fold(onSuccess={fetch(it)},onFailure={Result.failure(it)}) }
    class UnauthorizedException: Exception()
}
