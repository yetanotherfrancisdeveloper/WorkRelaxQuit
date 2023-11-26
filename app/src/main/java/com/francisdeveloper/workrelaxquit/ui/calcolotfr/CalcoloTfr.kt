package com.francisdeveloper.workrelaxquit.ui.calcolotfr

import com.francisdeveloper.workrelaxquit.databinding.FragmentCalcoloTfrBinding
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.R
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.FileInputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import java.io.File
import kotlin.properties.Delegates

class FragmentCalcoloTfrBinding : Fragment(), DatePickerDialog.OnDateSetListener {

    private var _binding: FragmentCalcoloTfrBinding? = null
    private val binding get() = _binding!!
    private lateinit var addSalaryButton: Button
    private lateinit var calculateButton: Button
    private lateinit var tfrResultEditText: String
    private lateinit var tfrGross: String
    private lateinit var tfrTax: String
    private lateinit var tfrAppreciated: String
    private lateinit var TFRAppreciationCoefficient: String
    private lateinit var tfrWoTaxes: String
    private lateinit var salaryEntryContainer: ViewGroup
    private var tfrPreviousYears by Delegates.notNull<Double>()

    private val salaryEntries = mutableListOf<SalaryEntry>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalcoloTfrBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addSalaryButton = view.findViewById(R.id.addSalaryButton)
        calculateButton = view.findViewById(R.id.calculateButton)
        salaryEntryContainer = view.findViewById(R.id.salaryEntryContainer)

        addSalaryButton.setOnClickListener {
            addSalaryEntry()
        }

        calculateButton.setOnClickListener {
            calculateTFR()
            val collapsedText = binding.collapsedText
            val expandedText = binding.expandedText
            val lineSeparator = binding.lineSeparator
            val collapsibleCardView = binding.collapsibleCardView
            collapsibleCardView.visibility = View.VISIBLE

            collapsedText.text = "TFR netto: \u20AC$tfrResultEditText"

            if (tfrPreviousYears > 0.0) {
                expandedText.text = "- TFR lordo: \u20AC$tfrGross\n" +
                                    "- Coefficiente di rivalutazione: $TFRAppreciationCoefficient%\n" +
                                    "- TFR rivalutato: \u20AC$tfrAppreciated\n" +
                                    "- Tasse: \u20AC$tfrTax\n" +
                                    "- TFR - tasse: \u20AC$tfrWoTaxes\n"
            } else {
                expandedText.text = "- TFR lordo: \u20AC$tfrGross\n" +
                                    "- Tasse: \u20AC$tfrTax\n" +
                                    "- TFR - tasse: \u20AC$tfrWoTaxes\n\n" +
                                    "Il coefficiente di rivalutazione viene applicato solo ai redditi percepiti negli anni precedenti."
            }

            collapsibleCardView.setOnClickListener {
                if (expandedText.visibility == View.VISIBLE) {
                    // Card is currently expanded, collapse it
                    collapsedText.visibility = View.VISIBLE
                    expandedText.visibility = View.GONE
                    lineSeparator.visibility = View.GONE
                } else {
                    // Card is currently collapsed, expand it
                    collapsedText.visibility = View.VISIBLE
                    expandedText.visibility = View.VISIBLE
                    lineSeparator.visibility = View.VISIBLE
                }
            }

            // By default, set the card to be collapsed initially
            collapsedText.visibility = View.VISIBLE
            expandedText.visibility = View.GONE
            lineSeparator.visibility = View.GONE
        }
    }

    private fun addSalaryEntry() {
        val salaryEntryView =
            layoutInflater.inflate(R.layout.item_salary_entry, salaryEntryContainer, false)
        val startDateEditText = salaryEntryView.findViewById<EditText>(R.id.startDateEditText)
        val endDateEditText = salaryEntryView.findViewById<EditText>(R.id.endDateEditText)
        val salaryEditText = salaryEntryView.findViewById<EditText>(R.id.salaryEditText)
        val deleteButton = salaryEntryView.findViewById<ImageButton>(R.id.deleteButton)

        startDateEditText.setOnClickListener {
            showDatePickerDialog(startDateEditText)
        }

        endDateEditText.setOnClickListener {
            showDatePickerDialog(endDateEditText)
        }

        deleteButton.setOnClickListener {
            removeSalaryEntry(salaryEntryView)
        }

        salaryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                calculateButton.isEnabled = s?.isNotEmpty() ?: false
            }
        })

        salaryEntryContainer.addView(salaryEntryView)
        salaryEntries.add(SalaryEntry(startDateEditText, endDateEditText, salaryEditText))
    }

    private fun removeSalaryEntry(salaryEntryView: View) {
        salaryEntryContainer.removeView(salaryEntryView)
        // Remove the salary entry from the list as well
        salaryEntries.removeAll { entry ->
            entry.startDateEditText == salaryEntryView.findViewById(R.id.startDateEditText)
                    && entry.endDateEditText == salaryEntryView.findViewById(R.id.endDateEditText)
                    && entry.salaryEditText == salaryEntryView.findViewById(R.id.salaryEditText)
        }
    }

    fun onDeleteButtonClick(view: View) {
        // Get the parent view (item_salary_entry) of the clicked delete button
        val salaryEntryView = view.parent as? View
        salaryEntryView?.let {
            removeSalaryEntry(it)
        }
    }

    private fun showDatePickerDialog(dateEditText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(), this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.tag = dateEditText // Use the tag to identify the clicked EditText
        datePickerDialog.show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = formatDate(year, month, dayOfMonth)
        val dateEditText = view?.tag as? EditText // Retrieve the clicked EditText using the tag
        dateEditText?.setText(selectedDate)
    }

    private fun formatDate(year: Int, month: Int, dayOfMonth: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return simpleDateFormat.format(calendar.time)
    }

    private fun getItalianMonthName(month: Int): String {
        val monthsInItalian = arrayOf(
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
        )
        return monthsInItalian[month - 1] // Month values are 1-based in Calendar
    }

    private fun readCellValue(filePath: String, rowIndex: Int, columnIndex: Int): String {
        val fileInputStream = FileInputStream(filePath)
        val workbook = HSSFWorkbook(fileInputStream)

        val sheet = workbook.getSheetAt(0)
        val row = sheet.getRow(rowIndex)
        val cell = row?.getCell(columnIndex)

        val cellValue = cell?.toString() ?: ""

        // workbook.close()
        fileInputStream.close()

        return cellValue
    }

    private fun getCellValueForYearMonth(filePath: String, year: Int, month: Int): Double? {
        var fileInputStream: FileInputStream? = null
        var workbook: HSSFWorkbook? = null

        try {
            fileInputStream = FileInputStream(filePath)
            workbook = HSSFWorkbook(fileInputStream)

            val sheet = workbook.getSheetAt(0) // Assuming the sheet is at index 0

            // Find the column index for the given month
            val monthRow = sheet.getRow(5) // Assuming months start in the sixth row
            var monthIndex = month
            for (colIndex in 1 until monthRow.lastCellNum) { // Start from the second column
                val cell = monthRow.getCell(colIndex).toString()
                if (cell == getMonthName(month)) {
                    monthIndex = colIndex
                    break
                }
            }

            // Find the row index for the given year
            var yearIndex = -1
            for (i in 8 until sheet.lastRowNum + 1) { // Assuming years start from the 9th row
                val row = sheet.getRow(i)
                val cell = row.getCell(0).toString()
                // If cell value isn't an integer then skip the row
                try {
                    cell.toInt()
                } catch (e: Exception) {
                    continue
                }

                if (cell.toInt() == year) {
                    yearIndex = i
                    break
                }
            }

            if (monthIndex != -1 && yearIndex != -1) {
                val cellValue = sheet.getRow(yearIndex).getCell(monthIndex).toString().toDouble()
                return cellValue
            }
        } catch (e: Exception) {
            Log.e("ExcelReader", "Error reading Excel file: ${e.message}", e)
        } finally {
            workbook = null
            fileInputStream?.close()
        }

        return null
    }

    private fun getMonthName(month: Int): String {
        val monthsInEnglish = arrayOf(
            "GEN", "FEB", "MAR", "APR", "MAG", "GIU",
            "LUG", "AGO", "SET", "OTT", "NOV", "DIC"
        )
        return monthsInEnglish[month - 1] // Month values are 1-based
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateTFR() {
        var filename = ""
        var year = 0
        var prevYearInflation = 0.0
        var currentInflation = 0.0
        val currentDate = Calendar.getInstance()
        val dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH)
        var enteredException = false

        if (dayOfMonth >= 10) {
            // If it's the 10th or later of the current month, proceed as usual
            year = currentDate.get(Calendar.YEAR)
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH))
            filename = "data_${year}_${italianMonth}.xls"
            val filePath = "${requireContext().filesDir}/${filename}"
            try {
                // If the file is not downloaded because ISTAT didn't update it, then use the one from the previous month
                currentInflation = getCellValueForYearMonth(filePath, year, currentDate.get(Calendar.MONTH))!!
                prevYearInflation = getCellValueForYearMonth(filePath, year - 1, 12)!!
            } catch (e: Exception) {
                enteredException = true
                currentDate.add(Calendar.MONTH, -1)
                year = currentDate.get(Calendar.YEAR)
                val italianMonthException = getItalianMonthName(currentDate.get(Calendar.MONTH))
                filename = "data_${year}_${italianMonthException}.xls"
                val filePathException = "${requireContext().filesDir}/${filename}"
                currentInflation = getCellValueForYearMonth(filePathException, year, currentDate.get(Calendar.MONTH))!!
                prevYearInflation = getCellValueForYearMonth(filePathException, year - 1, 12)!!
                Log.e("TFR", "Error opening file: ${e.message}", e)
            }
        } else {
            // If it's not the 10th of the current month, set the month to two months before the current one
            currentDate.add(Calendar.MONTH, -1)
            year = currentDate.get(Calendar.YEAR)
            val italianMonth = getItalianMonthName(currentDate.get(Calendar.MONTH))
            filename = "data_${year}_${italianMonth}.xls"
            val filePath = "${requireContext().filesDir}/${filename}"
            try {
                currentInflation = getCellValueForYearMonth(filePath, year, currentDate.get(Calendar.MONTH))!!
                prevYearInflation = getCellValueForYearMonth(filePath, year - 1, 12)!!
            } catch (e: Exception) {
                Log.e("TFR", "Error opening file: ${e.message}", e)
            }
        }

        val file = File(requireContext().filesDir, filename)

        var totalTFR = 0.0
        var totalTFRPreCurrentYear = 0.0
        val salaries : HashMap<String, Int> = HashMap<String, Int> ()
        val daysWorked = mutableListOf<Double>()
        var workedMonths = 0
        var lastDate = ""
        var notSubtracted = true
        for (entry in salaryEntries) {
            val salary = entry.salaryEditText.text.toString().toDoubleOrNull() ?: 0.0
            val startDate = entry.startDateEditText.text.toString()
            val endDate = entry.endDateEditText.text.toString()
            lastDate = endDate
            val result = calculateMonthsBetweenDates(startDate, endDate)
            salaries["$startDate - $endDate"] = salary.toInt()

            var totalMonths = 0
            var thirteenQuotas = 0.0
            var index = 1
            for ((months, monthsToEnd) in result) {
                // LocalDate.parse(startDate, dateFormatter).dayOfMonth
                if (index < result.size) {
                    totalMonths += monthsToEnd
                    thirteenQuotas += (salary / 13 / 12) * monthsToEnd / 13.5
                    index += 1
                } else {
                    totalMonths += monthsToEnd
                    index += 1
                }
            }

            workedMonths += totalMonths

            var tfr = 0.0
            var tfrPreCurrentYear = 0.0
            val monthsYears = getDatesInRange(startDate, endDate)
            var monthsIndex = 0
            for (date in monthsYears) {
                val refYear = date.year
                val refMonth = date.monthValue
                val workingDays = getWorkingDaysInMonth(refYear, refMonth)
                val monthLastDay = getLastDateOfMonth(date.year, date.monthValue)
                val monthFirstDay = LocalDate.of(date.year, date.monthValue, 1).toString()

                val workingDaysDone = when (monthsIndex) {
                    0 -> getWorkingDaysInRange(startDate, monthLastDay.toString())
                    monthsYears.size - 1 -> getWorkingDaysInRange(monthFirstDay, endDate)
                    else -> getWorkingDaysInRange(monthFirstDay, monthLastDay.toString())
                }

                val betterTFRQuota = (salary / 12 / 13.5) * (workingDaysDone.toDouble() / workingDays.toDouble())
                daysWorked.add(workingDaysDone.toDouble() / workingDays.toDouble())
                tfr += betterTFRQuota

                if (refYear < year) {
                    tfrPreCurrentYear += betterTFRQuota
                }

                monthsIndex += 1
            }

            var tfrQuota: Double
            if (notSubtracted) {
                tfrQuota = tfr - (salary * 0.005)
                notSubtracted = false
            } else {
                tfrQuota = tfr
            }

            if (tfrQuota < 0.0) {
                tfrQuota += (salary * 0.005)
            }
            val tfrQuotaPreCurrentYear = if (tfrPreCurrentYear > 0.0) {
                tfrPreCurrentYear - (salary * 0.005)
            } else {
                0.0
            }

            totalTFR += tfrQuota
            totalTFRPreCurrentYear += tfrQuotaPreCurrentYear
        }

        // Compute appreciation TFR from previous years
        val percentVariation = (((currentInflation.minus(prevYearInflation)).div((prevYearInflation))).times(0.75))
        val permVariation = if (dayOfMonth >= 10) {
            if (enteredException) {
                (0.015 * ((Calendar.getInstance().get(Calendar.MONTH).toDouble() - 1) / 12.0))
            } else {
                (0.015 * (Calendar.getInstance().get(Calendar.MONTH).toDouble() / 12.0))
            }
        } else {
            (0.015 * ((Calendar.getInstance().get(Calendar.MONTH).toDouble() - 1) / 12.0))
        }
        val appreciationCoefficient = permVariation + percentVariation

        // Format values
        val df = DecimalFormat("#.##")
        // Compute net TFR
        var refTFR = (totalTFR * 144) / workedMonths.toDouble()
        tfrGross = df.format(totalTFR)
        val parsedLastDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastDate)!!
        // Appreciation TFR
        tfrPreviousYears = totalTFRPreCurrentYear
        if (totalTFRPreCurrentYear > 0.0) {
            val preTFR = (totalTFR * 144) / (workedMonths.toDouble() - (parsedLastDate.month + 1).toDouble())
            val preTFROnTotal = totalTFR * (((workedMonths.toDouble() - (parsedLastDate.month + 1).toDouble())) / workedMonths.toDouble())
            val appreciationValue = preTFR * appreciationCoefficient
            val appreciationValueOnTotal = preTFROnTotal * appreciationCoefficient
            val netAppreciationValue = appreciationValue - (appreciationValue * 0.17)
            val netAppreciationValueOnTotal = appreciationValueOnTotal - (appreciationValueOnTotal * 0.17)
            refTFR += netAppreciationValue
            totalTFR += netAppreciationValueOnTotal
        }

        val tfrIRPEFTax = calculateIRPEFTax(refTFR)
        val refTax = tfrIRPEFTax / refTFR
        val taxTFR = totalTFR * refTax
        val netTFR = totalTFR - taxTFR

        val formattedTFR = df.format(netTFR)

        val dfCoefficient = DecimalFormat("#.####")

        tfrResultEditText = formattedTFR
        tfrAppreciated = df.format(totalTFR)
        TFRAppreciationCoefficient = dfCoefficient.format(appreciationCoefficient * 100)
        tfrTax = df.format(taxTFR)
        tfrWoTaxes = df.format(netTFR)
    }

    private fun calculateIRPEFTax(grossAnnualSalary: Double): Double {
        val taxBands = arrayOf(0.23, 0.25, 0.35, 0.43)
        val taxThresholds = arrayOf(15000.0, 28000.0, 50000.0, 50000.0)

        var remainingSalary = grossAnnualSalary
        var irpefTax = 0.0

        for (i in taxBands.indices) {
            if (remainingSalary <= 0) {
                break
            }

            val taxRate = taxBands[i]
            val taxThreshold = taxThresholds[i]

            val taxableAmount = if (remainingSalary <= taxThreshold) remainingSalary else taxThreshold
            val taxAmount = taxableAmount * taxRate

            irpefTax += taxAmount
            remainingSalary -= taxableAmount
        }

        return irpefTax
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLastDateOfMonth(year: Int, month: Int): LocalDate {
        // Set the day to the last day of the month (28, 29, 30, or 31)
        val lastDayOfMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        // Create a LocalDate object representing the last date of the month
        return LocalDate.of(year, month, lastDayOfMonth)
    }

    private fun isWorkingDay(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Check if the day is not a Saturday (Calendar.SATURDAY) or Sunday (Calendar.SUNDAY)
        return calendar.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
    }

    private fun getWorkingDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // Note: Calendar months are zero-based (0 - January, 11 - December)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var workingDays = 0

        for (day in 1..daysInMonth) {
            calendar.set(year, month - 1, day)
            val currentDate = calendar.time

            // Exclude weekends and public holidays
            if (isWorkingDay(currentDate)) {
                workingDays++
            }
        }

        return workingDays
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getWorkingDaysInRange(start: String, end: String): Int {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)

        var currentDate = startDate
        var workingDays = 0

        while (!currentDate.isAfter(endDate)) {
            if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++
            }
            currentDate = currentDate.plusDays(1)
        }

        return workingDays
    }

    private fun isPublicHoliday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) // Note: Calendar months are zero-based (0 - January, 11 - December)

        val italianHolidays = listOf(
            // Format: Month (0-based), Day
            Pair(0, 1),   // 1 gennaio
            Pair(0, 6),   // 6 gennaio
            Pair(3, 25),  // 25 aprile
            Pair(4, 1),   // 1 maggio
            Pair(5, 2),   // 2 giugno
            Pair(7, 15),  // 15 agosto
            Pair(10, 1),  // 1 novembre
            Pair(11, 8),  // 8 dicembre
            Pair(11, 25), // 25 dicembre
            Pair(11, 26), // 26 dicembre
            Pair(11, 31)  // 31 dicembre
        )

        return italianHolidays.contains(Pair(month, dayOfMonth))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDatesInRange(start: String, end: String): List<LocalDate> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val datesInRange = mutableListOf<LocalDate>()

        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            datesInRange.add(currentDate)
            currentDate = currentDate.plusMonths(1)
        }

        return datesInRange
    }

    private fun calculateMonthsBetweenDates(startDate: String, endDate: String): List<Pair<Int, Int>> {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate)!!

        val endCalendar = Calendar.getInstance()
        endCalendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(endDate)!!

        val endOfYearCalendar = Calendar.getInstance()
        endOfYearCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
        endOfYearCalendar.set(Calendar.DAY_OF_MONTH, 31)

        val result = mutableListOf<Pair<Int, Int>>()

        while (startCalendar.before(endCalendar) || startCalendar == endCalendar) {
            val remainingMonths = 12 - startCalendar.get(Calendar.MONTH)
            val totalMonths = if (startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)) {
                endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH) + 1
            } else {
                remainingMonths
            }
            result.add(Pair(remainingMonths, totalMonths))
            startCalendar.add(Calendar.MONTH, remainingMonths)
            startCalendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    data class SalaryEntry(
        val startDateEditText: EditText,
        val endDateEditText: EditText,
        val salaryEditText: EditText
    )
}