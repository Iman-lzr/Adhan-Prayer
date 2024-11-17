package com.example.salat



import android.content.Context

import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PrayerNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prayerName = inputData.getString("prayerName") ?: return Result.failure()

        // Ici, vous pouvez gérer la logique de notification pour chaque prière.
        sendNotification(prayerName)

        return Result.success()
    }

    private fun sendNotification(prayerName: String) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, "PRAYER_TIMES_CHANNEL")
            .setSmallIcon(R.drawable.nabawi)
            .setContentTitle("C'est l'heure de la prière")
            .setContentText("Il est temps de prier $prayerName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
