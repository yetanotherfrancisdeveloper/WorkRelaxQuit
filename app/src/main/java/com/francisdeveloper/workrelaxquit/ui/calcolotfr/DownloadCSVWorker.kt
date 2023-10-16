import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DownloadCsvWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        //Log.d("DownloadCsvWorker", "Starting worker execution")
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
        val year = inputData.getInt(KEY_YEAR, -1)
        val month = inputData.getString(KEY_MONTH)

        if (downloadUrl.isNullOrEmpty() || year == -1 || month.isNullOrEmpty()) {
            //Log.e("DownloadCsvWorker", "Invalid input data")
            return Result.failure()
        }

        // Construct the URL with the year and month
        val urlWithYearMonth = "$downloadUrl?meseA=$month&annoA=$year&tav=7"
        //Log.d("DownloadCsvWorker", "URL: $urlWithYearMonth")

        // Create a filename based on year and month
        val filename = "data_${year}_${month.toString().padStart(2, '0')}.xls"

        // Get the app's internal storage directory
        val internalStorageDir = applicationContext.filesDir

        // Create the file object to save the downloaded XLS
        val file = File(internalStorageDir, filename)

        // Get the list of all files in the internal storage directory
        val allFiles = internalStorageDir.listFiles()

        // Loop through the files and delete those that have a different month in the name
        allFiles?.forEach { existingFile ->
            val fileName = existingFile.name
            if (fileName.endsWith(".xls")) {
                val fileMonth = fileName.substringAfterLast("_").substring(0, 2) // Extract month from the file name

                if (fileMonth != month) {
                    val deleted = existingFile.delete()
                    if (!deleted) {
                        Log.e("DownloadCsvWorker", "Failed to delete existing file: $fileName")
                        return Result.failure()
                    } else {
                        //Log.d("DownloadCsvWorker", "Deleted existing file: $fileName")
                    }
                }
            }
        }

        // Check if the file already exists
        if (file.exists()) {
            //Log.d("DownloadCsvWorker", "File already exists: $filename")
            return Result.success()
        }

        // Create an OkHttp client and perform the download
        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .url(urlWithYearMonth)
            .build()

        //Log.d("DownloadCsvWorker", "Starting download: $filename")

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("DownloadCsvWorker", "Network request failed: ${response.code}")
                // Download from previous month
                val currentDate = Calendar.getInstance()
                currentDate.add(Calendar.MONTH, -1)
                val prevMonth = getItalianMonthName(currentDate.get(Calendar.MONTH))
                val updatedYear = currentDate.get(Calendar.YEAR)
                val newFilename = "data_${updatedYear}_${prevMonth.padStart(2, '0')}.xls"
                // Create the file object to save the downloaded XLS
                val newFile = File(internalStorageDir, newFilename)

                // Check if the file already exists
                if (newFile.exists()) {
                    Log.d("DownloadCsvWorker", "File already exists: $newFilename")
                    return Result.success()
                }

                val newUrlWithYearMonth = "$downloadUrl?meseA=$prevMonth&annoA=$updatedYear&tav=7"
                // Log.d("TFR", "currentDate: $currentDate")
                // Log.d("TFR", "newFilename: $newFilename")
                // Log.d("TFR", "newUrlWithYearMonth: $newUrlWithYearMonth")
                val newOkHttpClient = OkHttpClient()
                val newRequest = Request.Builder()
                    .url(newUrlWithYearMonth)
                    .build()

                try {
                    val newResponse = newOkHttpClient.newCall(newRequest).execute()
                    if (!newResponse.isSuccessful) {
                        return Result.failure()
                    }

                    val newResponseBody = newResponse.body
                    if (newResponseBody == null) {
                        Log.e("DownloadCsvWorker", "Response body is null")
                        return Result.failure()
                    }

                    // Save the downloaded XLS to the file
                    newResponseBody.byteStream().use { inputStream ->
                        FileOutputStream(newFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Log.d("DownloadCsvWorker", "XLS downloaded successfully")
                    return Result.success()
                } catch (e: Exception) {
                    Log.e("DownloadCsvWorker", "Error during download: ${e.message}", e)
                    return Result.failure()
                }

            }

            val responseBody = response.body
            if (responseBody == null) {
                Log.e("DownloadCsvWorker", "Response body is null")
                return Result.failure()
            }

            // Save the downloaded XLS to the file
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            //Log.d("DownloadCsvWorker", "XLS downloaded successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e("DownloadCsvWorker", "Error during download: ${e.message}", e)
            return Result.failure()
        }
    }

    private fun getItalianMonthName(month: Int): String {
        val monthsInItalian = arrayOf(
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
        )
        return monthsInItalian[month - 1] // Month values are 1-based in Calendar
    }

    companion object {
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_YEAR = "year"
        const val KEY_MONTH = "month"
    }
}