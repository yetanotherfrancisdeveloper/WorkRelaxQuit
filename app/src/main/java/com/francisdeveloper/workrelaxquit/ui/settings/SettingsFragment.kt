package com.francisdeveloper.workrelaxquit.ui.settings

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import java.io.File
import java.io.FileOutputStream


class SettingsFragment : PreferenceFragmentCompat() {

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with creating and saving the file
                saveCsvFile()
            } else {
                // Permission denied, inform the user
                Toast.makeText(
                    requireContext(),
                    "Permesso negato. Il file non può essere salvato.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val darkModeSwitch: SwitchPreferenceCompat? = findPreference("dark_mode_switch")
        darkModeSwitch?.setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
            val darkModeEnabled = newValue as Boolean
            setAppTheme(darkModeEnabled)

            // Set the flag in SharedPreferences to prevent landing page change
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor = sharedPref.edit()
            editor.putBoolean("shouldChangeLandingPage", false)
            editor.putBoolean("dark_mode_switch", darkModeEnabled) // Update the preference
            editor.apply()

            true
        }

        val currentApiVersion = Build.VERSION.SDK_INT
        val preferenceScreen = findPreference<PreferenceScreen>("settings_preferences")
        val sendNotificationPreference = findPreference<SwitchPreferenceCompat>("send_notification")
        val scheduleNotificationPreference = findPreference<SwitchPreferenceCompat>("schedule_notification")

        if (currentApiVersion < Build.VERSION_CODES.O) {
            // Remove the preference
            preferenceScreen?.removePreference(findPreference("send_notification")!!)
            sendNotificationPreference!!.isVisible = false
        }

        if (currentApiVersion >= Build.VERSION_CODES.O && currentApiVersion < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            sendNotificationPreference!!.summary = "Abilita le notifiche"
        }

        if (currentApiVersion < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Remove the preference
            preferenceScreen?.removePreference(findPreference("schedule_notification")!!)
            scheduleNotificationPreference!!.isVisible = false
        }

        // Send notifications switch case
        val notificationSwitch: SwitchPreferenceCompat? = findPreference("send_notification")
        notificationSwitch?.setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
            // Check for the POST_NOTIFICATIONS permission
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val previousValue = sharedPreferences.getBoolean("send_notification", false)

            requestPostNotificationsPermission()

            if (isPostNotificationsPermissionGranted()) {
                notificationSwitch.isChecked = true
                if (newValue != previousValue) {
                    sharedPreferences.edit().putBoolean("send_notification", true).apply()
                }
            } else {
                notificationSwitch.isChecked = false
                if (newValue != previousValue) {
                    sharedPreferences.edit().putBoolean("send_notification", false).apply()
                }
            }

            // Return false to prevent the preference value from changing if necessary
            newValue == previousValue
        }

        // Schedule notifications switch case
        val scheduleNotificationSwitch: SwitchPreferenceCompat? = findPreference("schedule_notification")
        scheduleNotificationSwitch?.setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
            // Check for the SCHEDULE_EXACT_ALARM permission
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val previousValue = sharedPreferences.getBoolean("schedule_notification", false)

            requestScheduledNotificationsPermission()
            if (isExactAlarmPermissionGranted()) {
                //scheduleNotificationSwitch.isChecked = true
                if (newValue != previousValue) {
                    sharedPreferences.edit().putBoolean("schedule_notification", true).apply()
                }
            } else {
                //scheduleNotificationSwitch.isChecked = false
                if (newValue != previousValue) {
                    sharedPreferences.edit().putBoolean("schedule_notification", false).apply()
                }
            }

            // Return false to prevent the preference value from changing if necessary
            newValue == previousValue
        }

        val downloadPreference: Preference? = findPreference("download_data")

        downloadPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11 and later, request the permission using the launcher
                requestPermissionSAF()
            } else {
                // For Android 10 and below, request the permission using the old method
                requestPermissionOld()
            }
            true
        }
    }

    private fun isExactAlarmPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            "android.permission.SCHEDULE_EXACT_ALARM"
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPostNotificationsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        } else {
            // On older versions, there's no need to check this permission
            true
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun requestScheduledNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_notification_dialog_settings, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            val enableButton = dialogView.findViewById<Button>(R.id.enableButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val notificationText = dialogView.findViewById<TextView>(R.id.messageTextView)
            val text = "Modificare il consenso a Work Relax Quit di programmare le notifiche?"
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
                val packageName = requireContext().packageName
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle the case where the settings activity is not found
                    /*Toast.makeText(
                        this,
                        "Impossibile aprire le impostazioni delle notifiche.",
                        Toast.LENGTH_SHORT
                    ).show()*/
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

    @SuppressLint("ResourceAsColor")
    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_notification_dialog_settings, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            val enableButton = dialogView.findViewById<Button>(R.id.enableButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val notificationText = dialogView.findViewById<TextView>(R.id.messageTextView)
            val text = "Modificare il consenso a Work Relax Quit di inviare notifiche?"
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
                val packageName = requireContext().packageName
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle the case where the settings activity is not found
                    /*Toast.makeText(
                        this,
                        "Impossibile aprire le impostazioni delle notifiche.",
                        Toast.LENGTH_SHORT
                    ).show()*/
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

    // Function to request WRITE_EXTERNAL_STORAGE permission
    private fun requestPermission() {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        requestPermissionLauncher.launch(permission)
    }

    // Function to request permission using SAF (for Android 11 and higher)
    private fun requestPermissionSAF() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/csv"
            putExtra(Intent.EXTRA_TITLE, "data.csv")
        }
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
    }

    // Function to request WRITE_EXTERNAL_STORAGE permission (for Android 10 and below)
    private fun requestPermissionOld() {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        val requestCode = 123 // You can use any unique request code

        // Request the permission using the old method
        requestPermissions(arrayOf(permission), requestCode)
    }

    // Function to create and save the CSV file
    private fun saveCsvFile() {
        val csvData = generateCsvData()

        try {
            val fileName = "data.csv"
            val storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11 and later, use Storage Access Framework
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/csv"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                startActivityForResult(intent, CREATE_DOCUMENT_REQUEST_CODE)
            } else {
                // For Android 10 and below, save the file directly
                val file = File(storageDir, fileName)
                val outputStream = FileOutputStream(file)
                outputStream.write(csvData.toByteArray())
                outputStream.close()

                // Notify the user that the file has been saved
                Toast.makeText(
                    requireContext(),
                    "File $fileName salvato nella cartella 'Downloads'!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            // Handle any errors that may occur during file creation
            Toast.makeText(
                requireContext(),
                "Impossibile salvare il file CSV.",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    // Request code for Create Document Intent
    private val CREATE_DOCUMENT_REQUEST_CODE = 123

    // Handle the result of the Create Document Intent
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->

                // Write data to the selected document
                writeDataToDocument(uri)
            }
        }
    }

    private fun writeDataToDocument(uri: Uri) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                val csvData = generateCsvData()
                // Write your CSV data to the document's output stream
                outputStream.write(csvData.toByteArray())
            }

            // Notify the user that the file has been saved
            Toast.makeText(
                requireContext(),
                "Il file è stato salvato correttamente!",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            // Handle any errors that may occur during file creation
            Toast.makeText(
                requireContext(),
                "Non è stato possibile salvare il file CSV.",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun generateCsvData(): String {
        val databaseHelper = DatabaseHelper(requireContext())
        val data = loadDataFromDatabase()

        val csvData = StringBuilder()
        csvData.append("Data,Ore,Tipo\n") // CSV header

        for (entry in data) {
            csvData.append("${entry[0]},${entry[1]},${entry[2]}\n")
        }

        // Initial data
        val firstRowData = databaseHelper.getFirstRow()
        csvData.append("\n")
        csvData.append("ferieIniziali,permessiIniziali,meseInserimento\n")
        csvData.append("${firstRowData?.ferie},${firstRowData?.permessi},${firstRowData?.initialDate}\n")

        return csvData.toString()
    }

    private fun loadDataFromDatabase(): List<List<Any>> {
        val databaseHelper = DatabaseHelper(requireContext())
        val mainTableData = databaseHelper.getAllData()
        val accumulatedTableData = databaseHelper.getAccumulatedData()
        val dataList = mutableListOf<List<Any>>()

        // Data inserted
        mainTableData?.use {
            while (mainTableData.moveToNext()) {
                val id = mainTableData.getInt(mainTableData.getColumnIndex(DatabaseHelper.COL_ID))
                val date = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_DATE))
                val value = mainTableData.getDouble(mainTableData.getColumnIndex(DatabaseHelper.COL_VALUE))
                val type = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_TYPE))
                dataList.add(listOf(date, value, type))
            }
        }

        // Data accumulated
        accumulatedTableData.forEach { accumulatedData ->
            val date = accumulatedData.date
            val ferieValue = accumulatedData.accFerie
            val permessiValue = accumulatedData.accPermessi
            dataList.add(listOf(date, ferieValue, "Ferie"))
            dataList.add(listOf(date, permessiValue, "Permessi"))
        }

        return dataList
    }

    private fun setAppTheme(darkModeEnabled: Boolean) {
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
