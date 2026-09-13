package com.example.subscriptions.work

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subscriptions.notifications.NotificationHelper
import com.example.subscriptions.storage.SecurePrefs

class SubscriptionCheckWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val expiry = SecurePrefs(applicationContext).expiry
        if (expiry == 0L) return Result.success()
        val difference = expiry - System.currentTimeMillis()
        val key = when {
            difference in 0..3_600_000L -> "notified_1h"
            difference in 3_600_001L..86_400_000L -> "notified_24h"
            difference < 0L -> "notified_expired"
            else -> return Result.success()
        }
        val alerts = applicationContext.getSharedPreferences("alerts", 0)
        if (!alerts.getBoolean(key, false)) {
            NotificationHelper.show(applicationContext, key)
            alerts.edit().putBoolean(key, true).apply()
        }
        return Result.success()
    }
}
