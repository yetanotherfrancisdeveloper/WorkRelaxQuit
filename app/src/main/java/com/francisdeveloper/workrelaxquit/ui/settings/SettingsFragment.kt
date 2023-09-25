package com.francisdeveloper.workrelaxquit.ui.settings

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Objects
import java.util.jar.Manifest


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
