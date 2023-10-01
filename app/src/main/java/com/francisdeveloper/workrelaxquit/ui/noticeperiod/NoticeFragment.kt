package com.francisdeveloper.workrelaxquit.ui.noticeperiod

import android.R
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.DatePicker
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.databinding.FragmentNoticeBinding
import android.widget.EditText
import android.widget.Spinner
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.MobileAds
import com.francisdeveloper.workrelaxquit.ui.gestore.getCurrentDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NoticeFragment : Fragment(), DatePickerDialog.OnDateSetListener {
    private var _binding: FragmentNoticeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarView: EditText
    private lateinit var dropdownMenu: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = binding.calendarView

        // Number input fields
        val noticeDaysText = binding.notice
        val stopDaysText = binding.stopDaysNumber

        // Set up listeners
        var selectedDate: String = getCurrentDate()
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Change this pattern to match your input format
        val outputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = inputDateFormat.parse(selectedDate)
        // selectedDate = outputDateFormat.format(date!!)
        calendarView.setText(outputDateFormat.format(date!!))
        // on below line we are initializing our variables.
        val dateEdt = binding.calendarView
        // on below line we are adding
        // click listener for our edit text.
        dateEdt.setOnClickListener {
            // on below line we are getting
            // the instance of our calendar.
            val c = Calendar.getInstance()
            // on below line we are getting
            // our day, month and year.
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            // on below line we are creating a
            // variable for date picker dialog.
            val datePickerDialog = DatePickerDialog(
                // on below line we are passing context.
                requireContext(),
                { view, year, monthOfYear, dayOfMonth ->
                    // on below line we are setting
                    // date to our edit text.
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year.toString())
                    selectedDate = year.toString() + "-" + (monthOfYear + 1) + "-" + dayOfMonth.toString()
                    dateEdt.setText(dat)
                },
                // on below line we are passing year, month
                // and day for the selected date in our date picker.
                year,
                month,
                day
            )
            // at last we are calling show
            // to display our date picker dialog.
            datePickerDialog.show()
        }

        val infoIcon = binding.helpIcon
        infoIcon.setOnClickListener {
            showInfoDialog()
        }

        /*val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())*/

        // Set initial button state based on input validation
        binding.calculateButton.isEnabled = isInputValid()

        // Set up listeners to update button state when the input fields change
        binding.notice.addTextChangedListener(textWatcher)
        binding.stopDaysNumber.addTextChangedListener(textWatcher)

        dropdownMenu = binding.dropdownMenu
        val options = arrayOf("Calendario", "Lavorativi")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        dropdownMenu.adapter = adapter

        binding.calculateButton.setOnClickListener {
            val daysToAdd = noticeDaysText.text.toString().toInt() + stopDaysText.text.toString().toInt()
            val selectedOption = dropdownMenu.selectedItem as String

            val lastDay = if (selectedOption == "Calendario") {
                convertDateFormat(addDaysUsingCalendar(selectedDate, daysToAdd))
            } else {
                convertDateFormat(addBusinessDaysUsingCalendar(selectedDate, daysToAdd))
            }

            val resultText = "Ultimo giorno: $lastDay"

            val collapsedText = binding.collapsedText
            val expandedText = binding.expandedText
            val lineSeparator = binding.lineSeparator
            val collapsibleCardView = binding.collapsibleCardView
            collapsibleCardView.visibility = View.VISIBLE

            collapsedText.text = resultText
            expandedText.text = "- I giorni di ferie, malattia, infortunio e maternità non sono inclusi nel " +
                    "periodo di preavviso. Per questo motivo questi vengono aggiunti al calcolo.\n" +
                    "- I giorni non feriali e i festivi sono inclusi nel periodo di preavviso, se " +
                    "da contratto è espresso che i giorni di preavviso sono di calendario (maggioranza dei casi).\n" +
                    "- Se da contratto è previsto che i giorni di preavviso siano lavorativi, allora bisogna " +
                    "escludere i sabati, le domeniche e i festivi dal calcolo."

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
        }
    }

    private fun showDatePickerDialog(dateEditText: EditText): Calendar {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(), this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.tag = dateEditText // Use the tag to identify the clicked EditText
        datePickerDialog.show()

        return calendar
    }

    private fun showInfoDialog() {
        // You can show a dialog or a tooltip here with additional information.
        // For example, you can use AlertDialog or PopupWindow.
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Giorni di preavviso")
            .setMessage("I giorni di preavviso possono essere di calendario o lavorativi. " +
                    "Dipende dal contratto, ma nella maggior parte dei casi si tratta di giorni di calendario." +
                    "Per giorni di calendario si intendono inclusi festività, sabati e domeniche, " +
                    "mentre per giorni lavorativi questi si intendono esclusi.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun convertDateFormat(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    private fun addDaysUsingCalendar(inputDate: String, daysToAdd: Int): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(inputDate)!!

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)

        return dateFormat.format(calendar.time)
    }

    private fun isHoliday(date: Date): Boolean {
        val easterMonday = calculateEasterMonday(date.year)
        val holidays = setOf(
            "${date.year}-01-01", "${date.year}-01-06", easterMonday, "${date.year}-04-25",
            "${date.year}-05-01", "${date.year}-06-02", "${date.year}-08-15", "${date.year}-11-1",
            "${date.year}-12-08", "${date.year}-12-25", "${date.year}-12-26")

        return date.toString() in holidays
    }

    private fun calculateEasterMonday(year: Int): String {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        val formattedMonth = if (month < 10) "0$month" else month.toString()
        val formattedDay = if (day < 10) "0$day" else day.toString()

        return "$year-$formattedMonth-$formattedDay"
    }

    private fun isWeekend(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    private fun addBusinessDaysUsingCalendar(inputDate: String, daysToAdd: Int): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(inputDate)!!

        var daysLeft = daysToAdd
        while (daysLeft > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            if (!isHoliday(calendar.time) && !isWeekend(calendar.time)) {
                daysLeft--
            }
        }

        return dateFormat.format(calendar.time)
    }

    private fun isInputValid(): Boolean {
        val ralText = binding.notice.text.toString()
        val hoursText = binding.stopDaysNumber.text.toString()

        return ralText.isNotBlank() && hoursText.isNotBlank()
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not used, leave it empty
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Update the button state whenever the text changes
            binding.calculateButton.isEnabled = isInputValid()
        }

        override fun afterTextChanged(s: Editable?) {
            // Not used, leave it empty
        }
    }

    override fun onStart() {
        super.onStart()
        val resetDate: String = getCurrentDate()
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Change this pattern to match your input format
        val outputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = inputDateFormat.parse(resetDate)
        binding.calendarView.setText(outputDateFormat.format(date!!))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}