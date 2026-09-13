package com.example.subscriptions.work

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subscriptions.notifications.NotificationHelper
import com.example.subscriptions.repository.SubscriptionRepository
import com.example.subscriptions.storage.SecurePrefs

class SubscriptionCheckWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val secure = SecurePrefs(applicationContext)
        if (secure.token != null) SubscriptionRepository(secure).refreshOrRelogin()
        val expiry = secure.expiry
        if (expiry == 0L) return Result.success()
        val alerts = applicationContext.getSharedPreferences("alerts", 0)
        if (alerts.getLong("alert_expiry", 0L) != expiry) {
            alerts.edit().clear().putLong("alert_expiry", expiry).apply()
        }
        val difference = expiry - System.currentTimeMillis()
        val key = when {
            difference < 0L -> "notified_expired"
            difference in 0..3_600_000L -> "notified_1h"
            difference in 3_600_001L..86_400_000L -> "notified_24h"
            difference in 86_400_001L..259_200_000L -> "notified_3d"
            difference in 259_200_001L..604_800_000L -> "notified_7d"
            else -> return Result.success()
        }
        if (!alerts.getBoolean(key, false)) {
            NotificationHelper.show(applicationContext, key)
            alerts.edit().putBoolean(key, true).apply()
        }
        return Result.success()
    }
}
