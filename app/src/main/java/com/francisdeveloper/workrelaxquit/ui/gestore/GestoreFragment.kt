package com.francisdeveloper.workrelaxquit.ui.gestore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.app.DatePickerDialog
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentGestoreBinding
import com.github.mikephil.charting.charts.LineChart
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.MobileAds
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class GestoreFragment : Fragment(), DataAdapter.DataUpdateListener, DatePickerDialog.OnDateSetListener {
    private var _binding: FragmentGestoreBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarView: EditText
    private lateinit var feriePermessiInput: EditText
    private lateinit var dropdownMenu: Spinner
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var chart: LineChart
    private lateinit var dataAdapter: DataAdapter
    private lateinit var dataList: MutableList<DataModel>

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        calendarView = binding.calendarView
        feriePermessiInput = binding.feriePermessiInput

        addButton = binding.addButton
        //chart = binding.chart

        /*val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())*/

        // Initialize data list and adapter
        dataList = mutableListOf()
        // Set up database helper
        databaseHelper = DatabaseHelper(requireContext())
        dataAdapter = DataAdapter(dataList, databaseHelper, this)

        // Disable the addButton initially since the input field is empty
        addButton.isEnabled = false

        binding.feriePermessiInput.addTextChangedListener(textWatcher)

        // Format values
        val df = DecimalFormat("#.##")

        // Set up chart
        //chart.setNoDataText("No data available")

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

        dropdownMenu = binding.dropdownMenu
        val options = arrayOf("Ferie", "Permesso")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dropdownMenu.adapter = adapter

        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()

        if (firstRowData != null) {
            // Update the TextViews with initial data
            updateTexts()

            addButton.setOnClickListener {
                // Handle button click and data input
                val input = feriePermessiInput.text.toString()
                val inputDouble = input.toDouble()
                val currentDate = selectedDate
                val selectedOption = dropdownMenu.selectedItem as String
                val newData = DataModel(currentDate, inputDouble, selectedOption)
                dataList.add(newData)
                dataAdapter.notifyDataSetChanged()

                // Save data in SQLite database
                val insertedId = databaseHelper.insertData(currentDate, inputDouble, selectedOption)
                if (insertedId != -1L) {
                    newData.id = insertedId.toInt()
                }

                // Clear input field
                val startFerie = firstRowData.ferie
                val startPermessi = firstRowData.permessi
                feriePermessiInput.text.clear()

                binding.remainingPermessiValue.text = df.format(calculateRemainingPermessi(startPermessi)).toString()
                binding.remainingFerieValue.text = df.format(calculateRemainingFerie(startFerie)).toString()

                // Update chart with saved data
                //updateChart(dataList)

                dataAdapter = DataAdapter(dataList, databaseHelper, this)
            }
        } else {
            addButton.setOnClickListener {
                // Handle button click and data input
                val input = feriePermessiInput.text.toString()
                val inputDouble = input.toDouble()
                val currentDate = selectedDate
                val selectedOption = dropdownMenu.selectedItem as String
                val newData = DataModel(currentDate, inputDouble, selectedOption)
                dataList.add(newData)
                dataAdapter.notifyDataSetChanged()

                // Save data in SQLite database
                val insertedId = databaseHelper.insertData(currentDate, inputDouble, selectedOption)
                if (insertedId != -1L) {
                    newData.id = insertedId.toInt()
                }

                // Clear input field
                feriePermessiInput.text.clear()
                val startFerie = 0.0
                val startPermessi = 0.0

                val df = DecimalFormat("#.##")
                binding.remainingPermessiValue.text = df.format(calculateRemainingPermessi(startPermessi)).toString()
                binding.remainingFerieValue.text = df.format(calculateRemainingFerie(startFerie)).toString()

                // Update chart with saved data
                //updateChart(dataList)

                // updateTexts()
            }
        }

        // Set click listeners for the cards to navigate to the com.example.calcoloferie.ui.detail.DetailFragment
        val startFerie = if (firstRowData != null) {
            firstRowData.ferie
        } else {
            0.0
        }
        binding.remainingFerieValue.text = df.format(calculateRemainingFerie(startFerie)).toString()
        binding.remainingFerieCard.setOnClickListener {
            val remainingFerie = calculateRemainingFerie(startFerie)
            openDetailFragment(remainingFerie, "Ferie")
        }

        val startPermessi = if (firstRowData != null) {
            firstRowData.permessi
        } else {
            0.0
        }

        binding.remainingPermessiValue.text = df.format(calculateRemainingPermessi(startPermessi)).toString()
        binding.remainingPermessiCard.setOnClickListener {
            val remainingPermessi = calculateRemainingPermessi(startPermessi)
            openDetailFragment(remainingPermessi, "Permesso")
        }

        // Load saved data from SQLite database
        val savedData = loadDataFromDatabase()
        dataList.addAll(savedData)
        dataAdapter.notifyDataSetChanged()

        // Update chart with saved data
        //updateChart(savedData)
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

    private fun calculateRemainingFerie(startValue: Double): Double {
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()

        val remainingFerie = startValue - sumUsedFerie + ferieAccumulated

        return remainingFerie
    }

    private fun calculateRemainingPermessi(startValue: Double): Double {
        // Get all used permessi
        val sumUsedPermessi = databaseHelper.getSumOfColumnValues("Permesso")
        // Get permessi hours
        val permessiAccumulated = databaseHelper.getSumOfAccumulatedPermessi()

        val remainingPermessi = startValue - sumUsedPermessi + permessiAccumulated

        return remainingPermessi
    }

    private fun loadDataFromDatabase(): List<DataModel> {
        val mainTableData = databaseHelper.getAllData()
        val accumulatedTableData = databaseHelper.getAccumulatedData()
        val combinedDataList = mutableListOf<DataModel>()

        mainTableData?.use {
            while (mainTableData.moveToNext()) {
                val id = mainTableData.getInt(mainTableData.getColumnIndex(DatabaseHelper.COL_ID))
                val date = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_DATE))
                val value = mainTableData.getDouble(mainTableData.getColumnIndex(DatabaseHelper.COL_VALUE))
                val type = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_TYPE))
                combinedDataList.add(DataModel(date, value, type, id))
            }
        }

        accumulatedTableData.forEach { accumulatedData ->
            val date = accumulatedData.date
            val ferieValue = accumulatedData.accFerie
            val permessiValue = accumulatedData.accPermessi
            combinedDataList.add(DataModel(date, ferieValue, "Ferie"))
            combinedDataList.add(DataModel(date, permessiValue, "Permesso"))
        }

        return combinedDataList
    }

    private fun openDetailFragment(remaining: Double, type: String) {
        val bundle = Bundle()
        bundle.putDouble("remaining", remaining)
        bundle.putString("type", type)
        findNavController().navigate(R.id.action_gestoreFragment_to_detailFragment, bundle)
    }

    private fun isInputValid(): Boolean {
        val input = binding.feriePermessiInput.text.toString()
        val date = binding.calendarView
        return input.isNotBlank() && (input.toDoubleOrNull() != null) && input.isNotEmpty() && date.toString() != ""
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not used, leave it empty
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Update the button state whenever the text changes
            binding.addButton.isEnabled = isInputValid()
        }

        override fun afterTextChanged(s: Editable?) {
            // Not used, leave it empty
        }
    }

    /*private fun updateChart(dataList: List<DataModel>) {
        val entries = mutableListOf<Entry>()
        for (i in dataList.indices) {
            entries.add(Entry(i.toFloat(), dataList[i].value.toFloat()))
        }
        val dataSet = LineDataSet(entries, "Data")
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }*/

    // Move the code for updating the texts to a separate method
    private fun updateTexts() {
        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()
        // Get all used hours of ferie
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get all used permessi
        val sumUsedPermessi = databaseHelper.getSumOfColumnValues("Permesso")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()
        // Get permessi hours
        val permessiAccumulated = databaseHelper.getSumOfAccumulatedPermessi()

        if (firstRowData != null) {
            // Access the data from the first row
            val ferie = firstRowData.ferie
            val permessi = firstRowData.permessi
        } else {
            val remainingFerie = 0 - sumUsedFerie + ferieAccumulated
            val remainingPermessi = 0 - sumUsedPermessi + permessiAccumulated
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
        // Clean up resources and references
        databaseHelper.close()
    }

    override fun onDataDeleted() {
        updateTexts()
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

data class DataModel(var date: String, var value: Double, val type: String, var id: Int = 0)

fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = Date()
    return dateFormat.format(currentDate)
}

class DataAdapter(private val dataList: MutableList<DataModel>, private val databaseHelper: DatabaseHelper, private val dataUpdateListener: DataUpdateListener) : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    interface DataUpdateListener {
        fun onDataDeleted()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reversedList = dataList.reversed()
        holder.bind(reversedList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val valueTextView: TextView = itemView.findViewById(R.id.valueTextView)
        private val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        init {
            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val deletedData = dataList[position]
                    dataList.removeAt(position)
                    notifyItemRemoved(position)

                    // Delete data from SQLite database
                    databaseHelper.deleteData(deletedData.id)

                    // Notify the listener that data is deleted
                    dataUpdateListener.onDataDeleted()
                }
            }
        }

        fun bind(data: DataModel) {
            // Set the data to the views
            dateTextView.text = "${data.date}: "
            valueTextView.text = "${data.value} ore "
            typeTextView.text = "di ${data.type}"
        }
    }
}