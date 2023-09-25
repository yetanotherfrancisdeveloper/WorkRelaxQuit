package com.francisdeveloper.workrelaxquit.ui.charts

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.R as androidR
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentChartBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import com.francisdeveloper.workrelaxquit.ui.gestore.getCurrentDate
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private lateinit var lineChart: LineChart
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dropdownMenu: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dropdown menu selected item
        dropdownMenu = binding.dataTypeSpinner
        val options = arrayOf("Ferie", "Permesso")
        val adapter = ArrayAdapter(requireContext(), androidR.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(androidR.layout.simple_spinner_dropdown_item)
        dropdownMenu.adapter = adapter

        val isDarkModeOn = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkModeOn) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        // Get a reference to the Resources object from the Fragment's context
        val resources = requireContext().resources

        // Retrieve a color by its resource ID
        val primaryColor = resources.getColor(R.color.main)
        val accentColor = resources.getColor(R.color.accent)

        var type = "Ferie"
        // Add an OnItemSelectedListener to the Spinner
        dropdownMenu.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                // Handle the item selection here
                type = options[position]
                // Now, selectedType contains the selected item ("Ferie" or "Permesso")
                // val type = dropdownMenu.selectedItem as String

                // Initialize database
                databaseHelper = DatabaseHelper(requireContext())
                // Get data
                val firstRowData = databaseHelper.getFirstRow()

                var ferie: Float
                var permessi: Float
                val startTimestamp: Float
                if (firstRowData != null) {
                    // Access the data from the first row
                    ferie = firstRowData.ferie.toFloat()
                    permessi = firstRowData.permessi.toFloat()
                    startTimestamp = (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(firstRowData.initialDate)?.time ?: 0L).toFloat()
                } else {
                    ferie = 0.0F
                    permessi = 0.0F
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_MONTH, 1) // Set the day to the 1st day of the month
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    startTimestamp = (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateFormat.format(calendar.time).toString())?.time ?: 0L).toFloat()
                }

                val insertedData = databaseHelper.getDataByType(type)
                val accumulatedData = databaseHelper.getAccumulatedDataByType(type)

                val realEntries = mutableListOf<Entry>()
                val dataList = mutableListOf<List<Any>>()

                if (type == "Ferie") {
                    realEntries.add(Entry(startTimestamp, ferie))
                } else {
                    realEntries.add(Entry(startTimestamp, permessi))
                }

                insertedData?.use {
                    while (insertedData.moveToNext()) {
                        val rowId = insertedData.getInt(insertedData.getColumnIndex(DatabaseHelper.COL_ID))
                        val date = insertedData.getString(insertedData.getColumnIndex(DatabaseHelper.COL_DATE))
                        val value = insertedData.getDouble(insertedData.getColumnIndex(DatabaseHelper.COL_VALUE))
                        val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
                        dataList.add(listOf(timestamp.toFloat(), value.toFloat(), "inserted"))
                    }
                }

                accumulatedData.forEach { accumulatedData ->
                    val date = accumulatedData.date
                    val value = if (type == "Ferie") accumulatedData.accFerie else accumulatedData.accPermessi
                    val dataId = accumulatedData.id
                    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
                    dataList.add(listOf(timestamp.toFloat(), value.toFloat(), "accumulated"))
                }

                val sortedData = dataList.sortedBy { element ->
                    val timestamp = element[0] as Float
                    timestamp
                }

                for (list in sortedData) {
                    if (list[2] == "inserted") {
                        var valueToInsert: Float
                        if (type == "Ferie") {
                            valueToInsert = ferie - list[1] as Float
                            ferie -= list[1] as Float
                        } else {
                            valueToInsert = permessi - list[1] as Float
                            permessi -= list[1] as Float
                        }
                        realEntries.add(Entry(list[0] as Float, valueToInsert))
                    } else {
                        var valueToInsert: Float
                        if (type == "Ferie") {
                            valueToInsert = ferie + list[1] as Float
                            ferie += list[1] as Float
                        } else {
                            valueToInsert = permessi + list[1] as Float
                            permessi += list[1] as Float
                        }
                        realEntries.add(Entry(list[0] as Float, valueToInsert))
                    }
                }

                // Initialize UI elements
                lineChart = view.findViewById(R.id.lineChart)
                lineChart.isHighlightPerDragEnabled = true

                /*val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_view_layout, null)
                val markerTextViewX = markerView.findViewById<TextView>(R.id.textViewXValue)
                val markerTextViewY = markerView.findViewById<TextView>(R.id.textViewYValue)
                lineChart.marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view_layout)
                lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    @SuppressLint("SetTextI18n")
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        Log.d("ChartIssue", "e: $e")
                        if (e != null) {
                            val xValue = e.x
                            val yValue = e.y

                            // Display the values in the custom marker view
                            markerTextViewX.text = "Data: $xValue"
                            markerTextViewY.text = "Ore: $yValue"

                            // Refresh the marker
                            lineChart.marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view_layout)
                        }
                    }

                    override fun onNothingSelected() {
                        // Handle the case where nothing is selected
                    }
                })*/

                // Initialize and configure the LineChart here
                // You can set labels, axis, and other chart properties
                lineChart.setTouchEnabled(true)
                lineChart.setPinchZoom(true)
                // enable scaling and dragging
                lineChart.isDragEnabled = true
                lineChart.setScaleEnabled(true)
                lineChart.setDrawGridBackground(false)
                lineChart.description.isEnabled = false
                lineChart.setDrawBorders(true)

                val xAxis = lineChart.xAxis
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MMM dd", Locale.US) // Adjust date format as needed

                    override fun getFormattedValue(value: Float): String {
                        // Convert the timestamp (X-axis value) back to a date string
                        val date = Date(value.toLong())
                        return dateFormat.format(date)
                    }
                }
                xAxis.setDrawAxisLine(true)
                xAxis.setDrawGridLines(true)
                // xAxis.textColor = Color.rgb(255, 192, 56)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.textSize = 10f
                xAxis.setCenterAxisLabels(true)
                // Colors
                xAxis.textColor = textColor
                // xAxis.textSize = textSize

                val yAxisLeft = lineChart.axisLeft
                yAxisLeft.setDrawGridLines(true)
                yAxisLeft.setDrawAxisLine(true)
                yAxisLeft.textColor = textColor // Set the color you want
                // leftAxis.textSize = textSize // Set the text size

                val yAxisRight = lineChart.axisRight
                yAxisRight.isEnabled = false

                // Add legend if needed
                val legend = lineChart.legend
                legend.textColor = textColor // Set the color you want
                // legend.textSize = textSize // Set the text size
                legend.isEnabled = true

                // Example: Create a dummy dataset and display it
                val dataSet = LineDataSet(realEntries, type)
                dataSet.setDrawCircles(true)
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f
                dataSet.fillAlpha = 100
                dataSet.setDrawFilled(true)
                dataSet.fillColor = accentColor
                dataSet.highLightColor = Color.rgb(244, 117, 117)
                dataSet.setDrawCircleHole(false)
                dataSet.fillFormatter = IFillFormatter { dataSet, dataProvider -> // change the return value here to better understand the effect
                    // return 0;
                    lineChart.axisLeft.axisMinimum
                }

                val dataSets = ArrayList<ILineDataSet>()
                dataSets.add(dataSet)
                val lineData = LineData(dataSets)
                lineData.setDrawValues(false)
                lineChart.data = lineData
                lineChart.invalidate()

                // You can perform any actions or updates based on the selected item here.
                // For example, you can update your UI or perform some logic.
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // This method is called when nothing is selected.
                // You can handle this case if needed.
            }
        }

        // To get the initially selected item:
        val initialSelectedType = options[dropdownMenu.selectedItemPosition]

        // Initialize database
        databaseHelper = DatabaseHelper(requireContext())
        // Get data
        val firstRowData = databaseHelper.getFirstRow()

        var ferie: Float
        var permessi: Float
        if (firstRowData != null) {
            // Access the data from the first row
            ferie = firstRowData.ferie.toFloat()
            permessi = firstRowData.permessi.toFloat()
        } else {
            ferie = 0.0F
            permessi = 0.0F
        }

        val insertedData = databaseHelper.getDataByType(type)
        val accumulatedData = databaseHelper.getAccumulatedDataByType(type)

        val realEntries = mutableListOf<Entry>()
        val dataList = mutableListOf<List<Any>>()
        val startTimestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2023-08-15")?.time ?: 0L

        if (type == "Ferie") {
            realEntries.add(Entry(startTimestamp.toFloat(), ferie))
        } else {
            realEntries.add(Entry(startTimestamp.toFloat(), permessi))
        }

        insertedData?.use {
            while (insertedData.moveToNext()) {
                val rowId = insertedData.getInt(insertedData.getColumnIndex(DatabaseHelper.COL_ID))
                val date = insertedData.getString(insertedData.getColumnIndex(DatabaseHelper.COL_DATE))
                val value = insertedData.getDouble(insertedData.getColumnIndex(DatabaseHelper.COL_VALUE))
                val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
                dataList.add(listOf(timestamp.toFloat(), value.toFloat(), "inserted"))
            }
        }

        accumulatedData.forEach { accumulatedData ->
            val date = accumulatedData.date
            val value = if (type == "Ferie") accumulatedData.accFerie else accumulatedData.accPermessi
            val dataId = accumulatedData.id
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
            dataList.add(listOf(timestamp.toFloat(), value.toFloat(), "accumulated"))
        }

        val sortedData = dataList.sortedBy { element ->
            val timestamp = element[0] as Float
            timestamp
        }

        for (list in sortedData) {
            if (list[2] == "inserted") {
                var valueToInsert: Float
                if (type == "Ferie") {
                    valueToInsert = ferie - list[1] as Float
                    ferie -= list[1] as Float
                } else {
                    valueToInsert = permessi - list[1] as Float
                    permessi -= list[1] as Float
                }
                realEntries.add(Entry(list[0] as Float, valueToInsert))
            } else {
                var valueToInsert: Float
                if (type == "Ferie") {
                    valueToInsert = ferie - list[1] as Float
                    ferie -= list[1] as Float
                } else {
                    valueToInsert = permessi - list[1] as Float
                    permessi -= list[1] as Float
                }
                realEntries.add(Entry(list[0] as Float, valueToInsert))
            }
        }

        // Initialize UI elements
        lineChart = view.findViewById(R.id.lineChart)
        // Set the MarkerView for the LineChart
        lineChart.isHighlightPerDragEnabled = true

        /*val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_view_layout, null)
        val markerTextViewX = markerView.findViewById<TextView>(R.id.textViewXValue)
        val markerTextViewY = markerView.findViewById<TextView>(R.id.textViewYValue)
        lineChart.marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view_layout)
        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            @SuppressLint("SetTextI18n")
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                Log.d("ChartIssue", "e: $e")
                if (e != null) {
                    val xValue = e.x
                    val yValue = e.y

                    // Display the values in the custom marker view
                    markerTextViewX.text = "Data: $xValue"
                    markerTextViewY.text = "Ore: $yValue"

                    // Refresh the marker
                    lineChart.marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view_layout)
                }
            }

            override fun onNothingSelected() {
                // Handle the case where nothing is selected
            }
        })*/

        // Initialize and configure the LineChart here
        // You can set labels, axis, and other chart properties
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        // enable scaling and dragging
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        lineChart.description.isEnabled = false
        lineChart.setDrawBorders(true)

        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MMM dd", Locale.US) // Adjust date format as needed

            override fun getFormattedValue(value: Float): String {
                // Convert the timestamp (X-axis value) back to a date string
                val date = Date(value.toLong())
                return dateFormat.format(date)
            }
        }
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(true)
        // xAxis.textColor = Color.rgb(255, 192, 56)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.setCenterAxisLabels(true)
        // xAxis.position = XAxis.XAxisPosition.BOTTOM
        // Colors
        xAxis.textColor = textColor
        // xAxis.textSize = textSize

        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.setDrawAxisLine(true)
        yAxisLeft.textColor = textColor // Set the color you want
        // leftAxis.textSize = textSize // Set the text size

        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = false

        // Add legend if needed
        val legend = lineChart.legend
        legend.textColor = textColor // Set the color you want
        // legend.textSize = textSize // Set the text size
        legend.isEnabled = true

        // Example: Create a dummy dataset and display it
        val dataSet = LineDataSet(realEntries, type)
        dataSet.setDrawCircles(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.fillAlpha = 100
        dataSet.setDrawFilled(true)
        dataSet.fillColor = accentColor
        dataSet.highLightColor = Color.rgb(244, 117, 117)
        dataSet.setDrawCircleHole(false)
        dataSet.fillFormatter = IFillFormatter { dataSet, dataProvider -> // change the return value here to better understand the effect
            // return 0;
            lineChart.axisLeft.axisMinimum
        }

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet)
        val lineData = LineData(dataSets)
        lineData.setDrawValues(false)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    @SuppressLint("ViewConstructor")
    class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
        // Custom marker view logic, if needed
    }
}