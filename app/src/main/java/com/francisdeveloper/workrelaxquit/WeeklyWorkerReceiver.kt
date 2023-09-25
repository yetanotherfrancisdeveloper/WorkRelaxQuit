package com.francisdeveloper.workrelaxquit

import com.francisdeveloper.workrelaxquit.ui.gestore.WeeklyNotificationWorker
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WeeklyWorkerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Enqueue your com.francisdeveloper.workrelaxquit.ui.gestore.InsertDataWorker here
        val insertDataWorkRequest = OneTimeWorkRequestBuilder<WeeklyNotificationWorker>()
            .setInitialDelay(0, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "com.francisdeveloper.workrelaxquit.ui.gestore.WeeklyNotificationWorker",
            ExistingWorkPolicy.REPLACE, // Replace if already exists
            insertDataWorkRequest
        )
    }
}
