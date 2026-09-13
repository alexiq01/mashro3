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
        val json=Gson().toJson(mapOf("username" to username,"password" to password,"language" to "en","session_id" to ""))
        var response=ApiFactory.api.login(ApiFactory.encoded(json)); if(!response.isSuccessful) response=ApiFactory.api.login(ApiFactory.plain(json))
        val token=response.body()?.token ?: throw IllegalStateException("اسم المستخدم أو كلمة المرور غير صحيحة")
        prefs.token=token; prefs.username=username; prefs.password=password; token
    }
    suspend fun autoLogin(): Result<String> = runCatching { val response=ApiFactory.api.autoLogin(); response.body()?.token ?: throw IllegalStateException("انتهت جلسة الدخول") }
    suspend fun fetch(token: String): Result<Subscription> = runCatching {
        val bearer="Bearer $token"; val u=ApiFactory.api.user(bearer); if(u.code()==401)throw UnauthorizedException(); val service=ApiFactory.api.service(bearer); val dash=ApiFactory.api.dashboard(bearer)
        if(!u.isSuccessful||!service.isSuccessful||!dash.isSuccessful)throw IllegalStateException("تعذر تحميل بيانات الاشتراك")
        val root=JsonObject(); u.body()?.data?.entrySet()?.forEach{root.add(it.key,it.value)}; service.body()?.data?.entrySet()?.forEach{root.add(it.key,it.value)}; dash.body()?.data?.entrySet()?.forEach{root.add(it.key,it.value)}
        val result=JsonUtils.subscription(root); prefs.expiry=result.expiryMillis; result
    }
    suspend fun refreshOrRelogin(): Result<Subscription> { val current=prefs.token; if(current!=null){fetch(current).onSuccess{return Result.success(it)}}; val u=prefs.username; val p=prefs.password; if(u.isNullOrBlank()||p.isNullOrBlank())return Result.failure(IllegalStateException("لم يتم تسجيل الدخول بعد")); return login(u,p).fold(onSuccess={fetch(it)},onFailure={Result.failure(it)}) }
    class UnauthorizedException: Exception()
}
