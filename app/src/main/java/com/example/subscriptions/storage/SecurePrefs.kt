package com.example.subscriptions.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePrefs(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(context, "secure_prefs", MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(), EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    var token: String? get() = prefs.getString("token", null); set(value) { prefs.edit().putString("token", value).apply() }
    var username: String? get() = prefs.getString("username", null); set(value) { prefs.edit().putString("username", value).apply() }
    var password: String? get() = prefs.getString("password", null); set(value) { prefs.edit().putString("password", value).apply() }
    var expiry: Long get() = prefs.getLong("expiry", 0L); set(value) { prefs.edit().putLong("expiry", value).apply() }
    fun clear() { prefs.edit().clear().apply() }
}
