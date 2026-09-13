package com.example.subscriptions.notifications

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import com.example.subscriptions.MainActivity

object NotificationHelper {
    fun show(c: Context, key: String) {
        val title = when (key) {
            "notified_7d" -> "🔔 اشتراكك ينتهي خلال 7 أيام"
            "notified_3d" -> "⚠️ اشتراكك ينتهي خلال 3 أيام"
            "notified_24h" -> "⚠️ اشتراكك ينتهي غداً"
            "notified_1h" -> "🔴 اشتراكك ينتهي بعد ساعة!"
            else -> "❌ انتهى اشتراكك"
        }
        val text = when (key) {
            "notified_7d", "notified_3d" -> "ننصحك بتجديد الاشتراك مبكراً لتجنب انقطاع الخدمة"
            "notified_24h", "notified_1h" -> "افتح التطبيق لمشاهدة تفاصيل اشتراكك وتجديده"
            else -> "انتهت مدة الاشتراك، افتح التطبيق لمشاهدة الخيارات المتاحة"
        }
        val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(c, "subscription_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(pi).build()
        c.getSystemService(NotificationManager::class.java).notify(key.hashCode(), notification)
    }
}
