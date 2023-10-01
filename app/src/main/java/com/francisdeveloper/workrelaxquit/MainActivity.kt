package com.francisdeveloper.workrelaxquit

//import android.content.ContentValues.TAG
//import com.google.android.gms.ads.MobileAds
//import com.google.android.ump.ConsentForm
//import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateSuccessListener
//import com.google.android.ump.ConsentRequestParameters
//import com.google.android.ump.UserMessagingPlatform

import DownloadCsvWorker
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.*
import com.francisdeveloper.workrelaxquit.databinding.ActivityMainBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.ump.ConsentInformation
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    private var shouldChangeLandingPage = true // Initialize with true

    private val REQUEST_CODE_SCHEDULE_ALARM = 456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set tag for under age of consent. false means users are not under age
        // of consent.
        /*val params = ConsentRequestParameters
            .Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()*/

        /*consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            OnConsentInfoUpdateSuccessListener {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity,
                    ConsentForm.OnConsentFormDismissedListener { loadAndShowError ->
                        // Consent gathering failed.
                        if (loadAndShowError != null) {
                            Log.w(
                                TAG, String.format(
                                    "%s: %s",
                                    loadAndShowError.errorCode,
                                    loadAndShowError.message
                                )
                            )
                        }

                        // Consent has been gathered.
                        if (consentInformation.canRequestAds()) {
                            //initializeMobileAdsSdk()
                        }
                    }
                )
            },
            ConsentInformation.OnConsentInfoUpdateFailureListener {
                    requestConsentError ->
                // Consent gathering failed.
                Log.w(TAG, String.format("%s: %s",
                    requestConsentError.errorCode,
                    requestConsentError.message))
            })*/

        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        //if (consentInformation.canRequestAds()) {
            //initializeMobileAdsSdk()
        //}
        // Reset consent
        // consentInformation.reset()

        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialize the DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        //MobileAds.initialize(this)

        // Check if this is the first launch
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Schedule the initial CSV download work
        scheduleInitialCsvDownloadWork()

        // Schedule the periodic CSV download work
        schedulePeriodicCsvDownloadWork()

        // Load the default night mode setting
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        shouldChangeLandingPage = sharedPref.getBoolean("shouldChangeLandingPage", true)
        val isDarkModeOn = sharedPref.getBoolean("dark_mode_switch", true)

        // Set the night mode based on the setting
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navView.itemIconTintList = null
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Check if data has been inserted
        val dataInserted = checkDataInserted()
        if (dataInserted && shouldChangeLandingPage) {
            val navHostFragment =
                (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment)
            val inflater = navHostFragment.navController.navInflater
            val graph = inflater.inflate(R.navigation.mobile_navigation)
            graph.setStartDestination(R.id.nav_gestore)
            navHostFragment.navController.graph = graph
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home,
            R.id.nav_calcolatore,
            R.id.nav_tfr,
            R.id.nav_valore_ferie,
            R.id.nav_notice_period,
            R.id.nav_gestore,
            R.id.nav_charts,
            R.id.nav_settings,
            R.id.nav_kofi
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Check if this is the first launch of the app
        sharedPreferences.edit().putBoolean("is_first_launch_splash", false).apply()
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)

        // Request the SCHEDULE_EXACT_ALARM permission if not granted
        if (!isExactAlarmPermissionGranted() && isFirstLaunch) {
            requestScheduledNotificationsPermission()
            scheduleMonthlyWorker()
            if (isExactAlarmPermissionGranted()) {
                sharedPreferences.edit().putBoolean("schedule_notification", true).apply()
            } else {
                sharedPreferences.edit().putBoolean("schedule_notification", false).apply()
            }
        } else {
            // Permission is granted, schedule alarms
            if (isExactAlarmPermissionGranted()) {
                sharedPreferences.edit().putBoolean("schedule_notification", true).apply()
                scheduleWeeklyWorker()
            } else {
                sharedPreferences.edit().putBoolean("schedule_notification", false).apply()
            }
            scheduleMonthlyWorker()
        }

        // Check for the POST_NOTIFICATIONS permission
        if (!isPostNotificationsPermissionGranted() && isFirstLaunch) {
            requestPostNotificationsPermission()
            if (isPostNotificationsPermissionGranted()) {
                sharedPreferences.edit().putBoolean("send_notification", true).apply()
            } else {
                sharedPreferences.edit().putBoolean("send_notification", false).apply()
            }
        } else {
            if (isPostNotificationsPermissionGranted()) {
                sharedPreferences.edit().putBoolean("send_notification", true).apply()
            } else {
                sharedPreferences.edit().putBoolean("send_notification", false).apply()
            }
        }
    }

    private fun scheduleMonthlyWorker() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val workerIntent = Intent(this, MonthlyWorkerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, workerIntent,
            PendingIntent.FLAG_IMMUTABLE)

        // Calculate the time to schedule the worker at 12:00 AM on the first day of the next month
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, 1) // Move to the next month
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Set to the first day of the month

        // Schedule the worker using AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun scheduleWeeklyWorker() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val workerIntent = Intent(this, WeeklyWorkerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, workerIntent,
            PendingIntent.FLAG_IMMUTABLE)

        // Calculate the time to schedule the worker at 6:00 AM on every Friday
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 18) // 6 p.m.
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        // If the current time is already past 6:00 PM on Friday, move to the next Friday
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1) // Move to the next week (next Friday)
        }

        // Schedule the worker using AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun isPostNotificationsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        } else {
            // On older versions, there's no need to check this permission
            true
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_notification_dialog, null)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val enableButton = dialogView.findViewById<Button>(R.id.enableButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val notificationText = dialogView.findViewById<TextView>(R.id.messageTextView)
            val text = "Consentire a Work Relax Quit di inviare notifiche?"
            // Create a SpannableStringBuilder to apply styles
            val builder = SpannableStringBuilder(text)
            // Define the start and end positions of the text to be styled
            val startIndex = text.indexOf("Work Relax Quit")
            val endIndex = startIndex + "Work Relax Quit".length
            // Apply a bold style to the specified text range
            builder.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            // Set the styled text in your TextView
            notificationText.text = builder

            enableButton.setOnClickListener {
                // Redirect the user to the system notification settings using deep linking
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle the case where the settings activity is not found
                    Toast.makeText(
                        this,
                        "Impossibile aprire le impostazioni delle notifiche.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } else {
            // Do nothing for older versions where this permission is not required
        }
    }


        // Check if the SCHEDULE_EXACT_ALARM permission is granted
    private fun isExactAlarmPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            "android.permission.SCHEDULE_EXACT_ALARM"
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("ResourceAsColor")
    private fun requestScheduledNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_notification_dialog, null)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val enableButton = dialogView.findViewById<Button>(R.id.enableButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val notificationText = dialogView.findViewById<TextView>(R.id.messageTextView)
            val text = "Consentire a Work Relax Quit di programmare le notifiche?"
            // Create a SpannableStringBuilder to apply styles
            val builder = SpannableStringBuilder(text)
            // Define the start and end positions of the text to be styled
            val startIndex = text.indexOf("Work Relax Quit")
            val endIndex = startIndex + "Work Relax Quit".length
            // Apply a bold style to the specified text range
            builder.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            // Set the styled text in your TextView
            notificationText.text = builder

            enableButton.setOnClickListener {
                // Redirect the user to the system notification settings using deep linking
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle the case where the settings activity is not found
                    Toast.makeText(
                        this,
                        "Impossibile aprire le impostazioni delle notifiche.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            if (isExactAlarmPermissionGranted()) {
                scheduleMonthlyWorker()
                scheduleWeeklyWorker()
            }
        } else {
            // Do nothing for older versions where this permission is not required
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if the dark mode setting has changed
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkModeOn = sharedPref.getBoolean("dark_mode_switch", true)

        // Set the night mode based on the setting
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Set the flag in SharedPreferences to true
        val editor = sharedPref.edit()
        //editor.putBoolean("isDarkModeOn", newDarkModeSetting)
        editor.putBoolean("shouldChangeLandingPage", true)
        editor.apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_drawer, menu)
        MenuCompat.setGroupDividerEnabled(menu!!, true)
        return true
    }

    // Function to load a fragment
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.drawer_layout, fragment)
        transaction.commit()
    }

    private fun checkDataInserted(): Boolean {
        val firstRowData = databaseHelper.getFirstRow()
        return firstRowData != null
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)

        // Initialize the Google Mobile Ads SDK.
        //MobileAds.initialize(this)
    }

    private fun getItalianMonthName(month: Int): String {
        val monthsInItalian = arrayOf(
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
        )
        return monthsInItalian[month - 1] // Month values are 1-based in Calendar
    }

    private fun scheduleInitialCsvDownloadWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val currentDate = Calendar.getInstance()
        val dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH)

        if (dayOfMonth >= 15) {
            // If it's the 15th of the current month, proceed as usual
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH))

            val inputData = workDataOf(
                DownloadCsvWorker.KEY_DOWNLOAD_URL to "https://rivaluta.istat.it/Rivaluta/Widget/TavoleStream.action",
                DownloadCsvWorker.KEY_YEAR to currentDate.get(Calendar.YEAR),
                DownloadCsvWorker.KEY_MONTH to italianMonth // Pass the Italian month name
            )

            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadCsvWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "InitialCsvDownloadWork",
                ExistingWorkPolicy.KEEP,
                downloadWorkRequest
            )
        } else {
            // If it's not the 15th of the current month, set the month to two months before the current one
            currentDate.add(Calendar.MONTH, -1)
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH))

            val inputData = workDataOf(
                DownloadCsvWorker.KEY_DOWNLOAD_URL to "https://rivaluta.istat.it/Rivaluta/Widget/TavoleStream.action",
                DownloadCsvWorker.KEY_YEAR to currentDate.get(Calendar.YEAR),
                DownloadCsvWorker.KEY_MONTH to italianMonth // Pass the Italian month name
            )

            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadCsvWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "InitialCsvDownloadWork",
                ExistingWorkPolicy.KEEP,
                downloadWorkRequest
            )
        }
    }

    private fun schedulePeriodicCsvDownloadWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val currentDate = Calendar.getInstance()
        val dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH)
        // Log.d("DownloadCsvWorker", "dayOfMonth: $dayOfMonth")

        if (dayOfMonth >= 15) {
            // If it's the 15th or later of the current month, proceed as usual
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH) + 1)

            val downloadWorkRequest = PeriodicWorkRequestBuilder<DownloadCsvWorker>(15, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        DownloadCsvWorker.KEY_DOWNLOAD_URL to "https://rivaluta.istat.it/Rivaluta/Widget/tavoleWidget.jsp",
                        DownloadCsvWorker.KEY_YEAR to currentDate.get(Calendar.YEAR),
                        DownloadCsvWorker.KEY_MONTH to italianMonth // Pass the Italian month name
                    )
                )
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodicCsvDownloadWork",
                ExistingPeriodicWorkPolicy.KEEP,
                downloadWorkRequest
            )
        } else {
            // If it's not the 15th of the current month, set the month to two months before the current one
            currentDate.add(Calendar.MONTH, -1)
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH) + 1)

            val downloadWorkRequest = PeriodicWorkRequestBuilder<DownloadCsvWorker>(15, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        DownloadCsvWorker.KEY_DOWNLOAD_URL to "https://rivaluta.istat.it/Rivaluta/Widget/tavoleWidget.jsp",
                        DownloadCsvWorker.KEY_YEAR to currentDate.get(Calendar.YEAR),
                        DownloadCsvWorker.KEY_MONTH to italianMonth // Pass the Italian month name
                    )
                )
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodicCsvDownloadWork",
                ExistingPeriodicWorkPolicy.KEEP,
                downloadWorkRequest
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Getter method to provide access to DatabaseHelper
    fun getDatabaseHelper(): DatabaseHelper {
        return databaseHelper
    }
}