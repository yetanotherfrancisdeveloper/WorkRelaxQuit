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
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsertDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Get the current date
        val currentDate = Calendar.getInstance().time
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentMonth = dateFormatter.format(currentDate)

        // Check if data for the current month already exists in the table
        val databaseHelper = DatabaseHelper(applicationContext)
        val existingData = databaseHelper.getDataForMonth(currentMonth)
        if (existingData.isEmpty()) {
            // Get first row of InitialData
            val firstRowData = databaseHelper.getFirstRow()
            if (firstRowData != null) {
                val giorniFerie = firstRowData.giorniFerie
                val orePermessi = firstRowData.permessiHours
                //Log.d("AccData", "Permessi: $orePermessi, Ferie: $giorniFerie")
                val accFerie = ((giorniFerie.toDouble() * 8) / 12).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
                val accPermessi = (orePermessi / 12).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()

                databaseHelper.insertAccData(accFerie, accPermessi, currentMonth)

                // Send a notification
                sendNotification()
            }
        }
        return Result.success()
    }

    private fun sendNotification() {
        val channelId = "DataUpdateChannel"
        val notificationId = 123 // Unique ID for the notification

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
                "Data Update Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.drawable.ic_monthly_update)
            .setContentTitle("Aggiornamento permessi e ferie")
            .setContentText("Hai nuove ore di permesso e ferie disponibili!")
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Remove the notification when clicked

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(notificationId, notificationBuilder.build())
        // Log.d("NotificationIssue", "Monthly sent")
    }
}