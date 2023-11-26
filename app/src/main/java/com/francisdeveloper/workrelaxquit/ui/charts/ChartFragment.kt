package com.francisdeveloper.workrelaxquit.ui.charts

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentChartBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.R as androidR


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
                    calendar.set(Calendar.HOUR_OF_DAY, 6)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    startTimestamp = (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateFormat.format(calendar.time).toString())?.time ?: 0L).toFloat()
                }

                val insertedData = databaseHelper.getDataByType(type)
                val accumulatedData = databaseHelper.getAccumulatedDataByType(type)

                val realEntries = mutableListOf<Entry>()
                val dataList = mutableListOf<List<Any>>()

                if (type == "Ferie") {
                    realEntries.add(Entry(startTimestamp, ferie, "inserted"))
                } else {
                    realEntries.add(Entry(startTimestamp, permessi, "inserted"))
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
                        val timestamp = list[0] as Float
                        val longTimestamp = timestamp.toLong()
                        // Create a Date object from the Long timestamp
                        val date = Date(longTimestamp)
                        // Add a day (24 hours) to the Date object
                        val oneDayInMillis: Long = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
                        val modifiedDate = Date(date.time + oneDayInMillis)
                        // Convert the modified Date back to a timestamp (if needed)
                        val modifiedTimestamp = modifiedDate.time.toFloat()
                        realEntries.add(Entry(modifiedTimestamp, valueToInsert))
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

                // Adding marker view
                val mv = YourMarkerView(requireContext(), R.layout.custom_marker_view_layout)
                // Set the marker to the chart
                lineChart.markerView = mv

                // Initialize and configure the LineChart
                lineChart.setTouchEnabled(true)
                lineChart.setPinchZoom(true)
                // Enable scaling and dragging
                lineChart.isDragEnabled = true
                lineChart.setScaleEnabled(true)
                lineChart.setDrawGridBackground(false)
                lineChart.description.isEnabled = false
                lineChart.setDrawBorders(true)

                val xAxis = lineChart.xAxis

                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MMM dd", Locale.US)

                    override fun getFormattedValue(value: Float): String {
                        // Convert the timestamp (X-axis value) back to a date string
                        val date = Date(value.toLong())
                        return dateFormat.format(date)
                    }
                }

                // Calculate the interval between the first and last date in milliseconds
                val firstDateMillis = realEntries.minByOrNull { it.x }?.x?.toLong() ?: 0L
                val lastDateMillis = realEntries.maxByOrNull { it.x }?.x?.toLong() ?: 0L
                val intervalMillis = lastDateMillis - firstDateMillis
                // Define a padding factor (percentage) for the left and right sides
                val paddingFactor = 0.12 // 12% padding on both sides
                // Calculate the padding amount based on the interval
                val paddingMillis = intervalMillis * paddingFactor
                // Calculate axisMinimum and axisMaximum
                val axisMinMillis = firstDateMillis - paddingMillis
                val axisMaxMillis = lastDateMillis + paddingMillis
                xAxis.axisMinimum = axisMinMillis.toFloat()
                xAxis.axisMaximum = axisMaxMillis.toFloat()

                xAxis.setDrawAxisLine(true)
                xAxis.setDrawGridLines(true)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.textSize = 10f
                xAxis.setCenterAxisLabels(true)
                // Colors
                xAxis.textColor = textColor

                val yAxisLeft = lineChart.axisLeft
                yAxisLeft.setDrawGridLines(true)
                yAxisLeft.setDrawAxisLine(true)
                yAxisLeft.textColor = textColor

                val yAxisRight = lineChart.axisRight
                yAxisRight.isEnabled = false

                // Add legend if needed
                val legend = lineChart.legend
                legend.textColor = textColor
                legend.isEnabled = true

                val dataSet = LineDataSet(realEntries, type)
                dataSet.setDrawCircles(true)
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f
                dataSet.fillAlpha = 100
                dataSet.setDrawFilled(true)
                dataSet.fillColor = accentColor
                dataSet.highLightColor = Color.rgb(244, 117, 117)
                dataSet.setDrawCircleHole(false)
                dataSet.fillFormatter = IFillFormatter { dataSet, dataProvider ->
                    lineChart.axisLeft.axisMinimum
                }

                val dataSets = ArrayList<ILineDataSet>()
                dataSets.add(dataSet)
                val lineData = LineData(dataSets)
                lineData.setDrawValues(false)
                lineChart.data = lineData
                lineChart.invalidate()

            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // This method is called when nothing is selected.
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
            calendar.set(Calendar.HOUR_OF_DAY, 6)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            startTimestamp = (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateFormat.format(calendar.time).toString())?.time ?: 0L).toFloat()
        }

        val insertedData = databaseHelper.getDataByType(type)
        val accumulatedData = databaseHelper.getAccumulatedDataByType(type)

        val realEntries = mutableListOf<Entry>()
        val dataList = mutableListOf<List<Any>>()

        if (type == "Ferie") {
            realEntries.add(Entry(startTimestamp, ferie, "inserted"))
        } else {
            realEntries.add(Entry(startTimestamp, permessi, "inserted"))
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
                val timestamp = list[0] as Float
                val longTimestamp = timestamp.toLong()
                // Create a Date object from the Long timestamp
                val date = Date(longTimestamp)
                // Add a day (24 hours) to the Date object
                val oneDayInMillis: Long = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
                val modifiedDate = Date(date.time + oneDayInMillis)
                // Convert the modified Date back to a timestamp (if needed)
                val modifiedTimestamp = modifiedDate.time.toFloat()
                realEntries.add(Entry(modifiedTimestamp, valueToInsert))
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

        // Adding marker view
        val mv = YourMarkerView(requireContext(), R.layout.custom_marker_view_layout)
        // Set the marker to the chart
        lineChart.markerView = mv

        // Initialize and configure the LineChart
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        // Enable scaling and dragging
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        lineChart.description.isEnabled = false
        lineChart.setDrawBorders(true)

        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MMM dd", Locale.US)

            override fun getFormattedValue(value: Float): String {
                // Convert the timestamp (X-axis value) back to a date string
                val date = Date(value.toLong())
                return dateFormat.format(date)
            }
        }

        // Calculate the interval between the first and last date in milliseconds
        val firstDateMillis = realEntries.minByOrNull { it.x }?.x?.toLong() ?: 0L
        val lastDateMillis = realEntries.maxByOrNull { it.x }?.x?.toLong() ?: 0L
        val intervalMillis = lastDateMillis - firstDateMillis
        // Define a padding factor (percentage) for the left and right sides
        val paddingFactor = 0.12 // 12% padding on both sides
        // Calculate the padding amount based on the interval
        val paddingMillis = intervalMillis * paddingFactor
        // Calculate axisMinimum and axisMaximum
        val axisMinMillis = firstDateMillis - paddingMillis
        val axisMaxMillis = lastDateMillis + paddingMillis
        xAxis.axisMinimum = axisMinMillis.toFloat()
        xAxis.axisMaximum = axisMaxMillis.toFloat()

        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.setCenterAxisLabels(true)
        // Colors
        xAxis.textColor = textColor

        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.setDrawAxisLine(true)
        yAxisLeft.textColor = textColor

        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = false

        // Add legend
        val legend = lineChart.legend
        legend.textColor = textColor
        legend.isEnabled = true

        val dataSet = LineDataSet(realEntries, type)
        dataSet.setDrawCircles(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.fillAlpha = 100
        dataSet.setDrawFilled(true)
        dataSet.fillColor = accentColor
        dataSet.highLightColor = Color.rgb(244, 117, 117)
        dataSet.setDrawCircleHole(false)
        dataSet.fillFormatter = IFillFormatter { dataSet, dataProvider ->
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
    class YourMarkerView(context: Context?, layoutResource: Int) : MarkerView(context,
        layoutResource
    ) {
        private var dateValue: TextView? = findViewById(R.id.textViewXValue)
        private var hourValue: TextView? = findViewById(R.id.textViewYValue)

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        override fun refreshContent(e: Entry, highlight: Highlight) {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
            val date = Date(e.x.toLong())
            // Add one day to the date
            val calendar = Calendar.getInstance()
            calendar.time = date
            if (e.data == "inserted") {
                calendar.add(Calendar.DAY_OF_MONTH, 1) // Add one day
            }
            dateValue!!.text = "Data: " + dateFormat.format(calendar.time)
            val df = DecimalFormat("#.##")
            hourValue!!.text = "Ore: " + df.format(e.y).toString()
            // this will perform necessary layouting
            super.refreshContent(e, highlight)
        }

        private var mOffset: MPPointF? = null
        override fun getOffset(): MPPointF {
            if (mOffset == null) {
                // Center the marker horizontally and vertically
                mOffset = MPPointF(-(width / 2).toFloat(), -height.toFloat())
            }
            return mOffset as MPPointF
        }
    }
}