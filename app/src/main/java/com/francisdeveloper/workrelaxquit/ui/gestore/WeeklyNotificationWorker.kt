package com.francisdeveloper.workrelaxquit.ui.gestore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.francisdeveloper.workrelaxquit.MainActivity
import com.francisdeveloper.workrelaxquit.R

class WeeklyNotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Build the notification
        val channelId = "WeeklyUpdateChannel"
        val notificationId = 456 // Unique ID for the notification

        val intent = Intent(applicationContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a notification channel (for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weekly Update Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Notifica settimanale")
            .setContentText("Ricorda di inserire ferie e permessi usati questa settimana!")
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_friday_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Remove the notification when clicked
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(notificationId, notification)
        // Log.d("NotificationIssue", "Notification sent")

        return Result.success()
    }
}