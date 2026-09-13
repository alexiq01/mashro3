package com.example.subscriptions

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.example.subscriptions.storage.SecurePrefs
import com.example.subscriptions.work.SubscriptionCheckWorker
import java.util.concurrent.TimeUnit

class App : Application() {
 override fun onCreate() { super.onCreate(); if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("subscription_alerts","تنبيهات الاشتراك",NotificationManager.IMPORTANCE_HIGH));
  val request=PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(15,TimeUnit.MINUTES).build(); WorkManager.getInstance(this).enqueueUniquePeriodicWork("subscription_check",ExistingPeriodicWorkPolicy.KEEP,request)
 }
}
