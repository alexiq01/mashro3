package com.example.subscriptions.notifications
import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import com.example.subscriptions.MainActivity
import com.example.subscriptions.R
object NotificationHelper { fun show(c:Context,key:String){val title=when(key){"notified_24h"->"⚠️ اشتراكك ينتهي غداً";"notified_1h"->"🔴 اشتراكك ينتهي بعد ساعة!";else->"❌ انتهى اشتراكك"};val pi=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); val n=NotificationCompat.Builder(c,"subscription_alerts").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText("افتح التطبيق لمشاهدة تفاصيل اشتراكك").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi).build(); c.getSystemService(NotificationManager::class.java).notify(key.hashCode(),n)} }
