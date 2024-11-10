package com.example.salat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PrayerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayerName") ?: "Prayer"

        val notificationBuilder = NotificationCompat.Builder(context, "PRAYER_TIMES_CHANNEL")
            .setSmallIcon(R.drawable.nabawi)
            .setContentTitle("Time for $prayerName")
            .setContentText("It's time for $prayerName prayer.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(prayerName.hashCode(), notificationBuilder.build())
    }
}
