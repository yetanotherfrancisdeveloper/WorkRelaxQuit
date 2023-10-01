package com.francisdeveloper.workrelaxquit.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentDetailBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DataModel
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import com.francisdeveloper.workrelaxquit.ui.home.AccDataModel
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private var menuDetailItem: MenuItem? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var detailDataAdapter: DetailDataAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var type: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.detailRecyclerView

        // Retrieve type from arguments
        type = arguments?.getString("type").toString()

        // Set the header text based on the data type
        if (type == "Ferie") {
            (activity as AppCompatActivity).supportActionBar?.title = "Ferie"
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = "Permessi"
        }

        /*val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())*/

        // Set up database helper
        databaseHelper = DatabaseHelper(requireContext())
        // Initialize data adapter
        detailDataAdapter = DetailDataAdapter(requireContext())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = detailDataAdapter
        }

        // Set up swipe-to-delete functionality
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(requireContext(), detailDataAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Load saved data from the database and update the adapter
        val (dataList, monthHeaders, groupedData) = loadDataFromDatabase()
        if (dataList.isEmpty()) {
            binding.simpleCardView.visibility = View.VISIBLE
            if (type == "Ferie") {
                binding.missingValuesText.text = "Non hai ancora inserito o maturato ferie!"
            } else {
                binding.missingValuesText.text = "Non hai ancora inserito o maturato permessi!"
            }
        }
        val uniqueMonthHeaders = getUniqueMonths(groupedData)
        detailDataAdapter.setData(dataList, uniqueMonthHeaders, groupedData)
    }

    private fun getUniqueMonths(dataList: List<Any>): List<String> {
        val uniqueMonths = mutableListOf<String>()
        var currentMonth = ""

        for (data in dataList) {
            if (data is String && data != currentMonth) {
                uniqueMonths.add(data)
                currentMonth = data
            }
        }

        return uniqueMonths
    }

    private fun loadDataFromDatabase(): Triple<List<DataModel>, List<String>, List<Any>> {
        val mainTableData = databaseHelper.getDataByType(type)
        val accumulatedTableData = databaseHelper.getAccumulatedDataByType(type)

        val dataList = mutableListOf<DataModel>()
        val monthHeaders = mutableListOf<String>()

        mainTableData?.use {
            while (mainTableData.moveToNext()) {
                val rowId = mainTableData.getInt(mainTableData.getColumnIndex(DatabaseHelper.COL_ID))
                val date = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_DATE))
                val value = mainTableData.getDouble(mainTableData.getColumnIndex(DatabaseHelper.COL_VALUE))
                val entryType = mainTableData.getString(mainTableData.getColumnIndex(DatabaseHelper.COL_TYPE))
                dataList.add(DataModel(date, value, entryType, rowId))
            }
        }

        accumulatedTableData.forEach { accumulatedData ->
            val date = accumulatedData.date
            val value = if (type == "Ferie") accumulatedData.accFerie else accumulatedData.accPermessi
            val dataId = accumulatedData.id
            if (type == "Ferie") {
                dataList.add(DataModel(date, value, "accFerie", dataId))
            } else {
                dataList.add(DataModel(date, value, "accPermesso", dataId))
            }
        }

        val groupedData = groupDataByMonth(dataList)
        for (item in groupedData) {
            if (item is String) {
                monthHeaders.add(item)
            }
        }

        return Triple(dataList, monthHeaders, groupedData)
    }

    private fun groupDataByMonth(dataList: List<Any>): List<Any> {
        val groupedData = mutableListOf<Any>()
        var currentMonth = ""

        for (data in dataList) {
            if (data is DataModel) {
                val month = getMonthFromDate(data.date)
                if (month != currentMonth) {
                    groupedData.add(month)
                    currentMonth = month
                }
                groupedData.add(data)
            }
        }

        return groupedData
    }

    private fun getMonthFromDate(date: String): String {
        // Assuming date format is "YYYY-MM-DD"
        val parts = date.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // Months in Calendar are 0-based, so subtract 1 from the month
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)

            // Format the month using SimpleDateFormat
            val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return monthFormatter.format(calendar.time)
        }

        return ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DetailDataAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_DATA = 1
    }
    private var itemList: List<Any> = emptyList() // Use Any type for a mixed list of data and headers
    private lateinit var monthList: List<String>
    private var dataList: List<DataModel> = emptyList()
    // For undoing to deletion
    private var recentlyDeletedItem: DataModel? = null
    private var recentlyDeletedItemAcc: AccDataModel? = null
    private var recentlyDeletedItemPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            TYPE_DATA -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_data, parent, false)
                DataViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DataViewHolder -> {
                val item = itemList[position]
                if (item is DataModel) {
                    holder.bind(item)
                }
            }
            is MonthHeaderViewHolder -> {
                val item = itemList[position]
                if (item is String) {
                    holder.bind(item)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = itemList[position]
        return if (item is String) {
            TYPE_MONTH_HEADER
        } else {
            TYPE_DATA
        }
    }

    // Function to get data at a specific position
    fun getDataAtPosition(position: Int): DataModel {
        val reversedPosition = dataList.size - 1 - position // Reverse the position
        return itemList[position] as DataModel
    }

    override fun getItemCount(): Int {
        // The total count includes both data items and month headers
        return itemList.size
    }

    fun setData(newDataList: List<DataModel>, newMonthList: List<String>, groupedData: List<Any>) {
        dataList = newDataList
        monthList = newMonthList

        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // Group data by month using a HashMap
        val monthOtherMap = hashMapOf<Date, MutableList<DataModel>>()
        for (item in groupedData) {
            if (item is DataModel) {
                val inputDate = inputDateFormat.parse(item.date)
                monthOtherMap.computeIfAbsent(inputDate) { mutableListOf() }.add(item)
            }
        }

        // Sort the keys (months) based on their dates
        val sortedMonths = monthOtherMap.keys.sortedDescending() // Ascending order

        // Create a combined list with month headers and sorted data
        val combinedDataList = mutableListOf<Any>()
        // val sortedMonths = monthDataMap.keys.sortedDescending()
        for (month in sortedMonths) {
            val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputDateStr = outputDateFormat.format(month)
            combinedDataList.add(getMonthFromDate(outputDateStr))

            val monthData = monthOtherMap[month]
            if (monthData != null) {
                combinedDataList.addAll(monthData.sortedByDescending { it.date })
            }
        }

        var combinedDataListFixed = mutableListOf<Any>()
        if (combinedDataList.size > 0) {
            val uniqueDates = getUniqueMonthYearValues(combinedDataList)
            val firstDates = mutableListOf<String>()
            firstDates.add(combinedDataList[0].toString())
            combinedDataListFixed.add(combinedDataList[0].toString())
            combinedDataList.drop(1).forEachIndexed { index, currentItem ->
                // Your code to process the currentItem
                if (currentItem in firstDates) {
                    return@forEachIndexed
                } else {
                    if (currentItem in uniqueDates && currentItem !in firstDates) {
                        firstDates.add(currentItem.toString())
                        combinedDataListFixed.add(currentItem)
                    } else {
                        combinedDataListFixed.add(currentItem)
                    }
                }
            }
        } else {
            combinedDataListFixed = combinedDataList
        }

        itemList = combinedDataListFixed
        notifyDataSetChanged()

        //itemList = groupedData
        //notifyDataSetChanged()
    }

    private fun getUniqueMonthYearValues(combinedDataList: List<Any>): Set<String> {
        val uniqueMonthYearValues = HashSet<String>()

        for (item in combinedDataList) {
            if (item is String && item.matches("[A-Za-z]+ \\d{4}".toRegex())) {
                // Assuming the format is "Month Year" (e.g., "November 2023")
                uniqueMonthYearValues.add(item)
            }
        }

        return uniqueMonthYearValues
    }

    private fun getMonthFromDate(date: String): String {
        // Assuming date format is "YYYY-MM-DD"
        val parts = date.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // Months in Calendar are 0-based, so subtract 1 from the month
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)

            // Format the month using SimpleDateFormat
            val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return monthFormatter.format(calendar.time)
        }

        return ""
    }

    fun deleteItem(position: Int) {
        // Delete the item from the list
        val updatedDataList = itemList.toMutableList()
        updatedDataList.removeAt(position)
        itemList = updatedDataList
        notifyItemRemoved(position)
        notifyDataSetChanged()
    }

    fun undoDelete() {
        val databaseHelper = DatabaseHelper(context)
        if (recentlyDeletedItem != null && recentlyDeletedItemPosition != -1) {
            // Add back the item in the database
            val insertedId = databaseHelper.insertData(recentlyDeletedItem!!.date, recentlyDeletedItem!!.value, recentlyDeletedItem!!.type)

            // Restore the item to its original position
            val updatedDataList = itemList.toMutableList()
            updatedDataList.add(recentlyDeletedItemPosition, recentlyDeletedItem!!)
            itemList = updatedDataList
            notifyItemInserted(recentlyDeletedItemPosition)
            notifyDataSetChanged()

            // Reset the recently deleted item
            recentlyDeletedItem = null
            recentlyDeletedItemPosition = -1
        }
    }

    fun undoDeleteAcc() {
        val databaseHelper = DatabaseHelper(context)
        if (recentlyDeletedItemAcc != null && recentlyDeletedItemPosition != -1) {

            // Add back the item in the database
            val insertedId = databaseHelper.insertAccData(recentlyDeletedItemAcc!!.accFerie, recentlyDeletedItemAcc!!.accPermessi, recentlyDeletedItemAcc!!.date)

            // Restore the item to its original position
            val updatedDataList = itemList.toMutableList()
            updatedDataList.add(recentlyDeletedItemPosition, recentlyDeletedItem!!)
            itemList = updatedDataList
            notifyItemInserted(recentlyDeletedItemPosition)
            notifyDataSetChanged()

            // Reset the recently deleted item
            recentlyDeletedItem = null
            recentlyDeletedItemPosition = -1
        }
    }

    fun getRecentlyDeletedItem(): DataModel? {
        return recentlyDeletedItem
    }

    fun setRecentlyDeletedItem(newRecentlyDeletedItem: DataModel) {
        recentlyDeletedItem = newRecentlyDeletedItem
    }

    fun setRecentlyDeletedAccItem(newRecentlyDeletedAccItem: AccDataModel) {
        recentlyDeletedItemAcc = newRecentlyDeletedAccItem
    }

    fun getRecentlyDeletedItemPosition(): Int {
        return recentlyDeletedItemPosition
    }

    fun setRecentlyDeletedItemPosition(newRecentlyDeletedItemPosition: Int) {
        recentlyDeletedItemPosition = newRecentlyDeletedItemPosition
    }

    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthHeaderTextView: TextView = itemView.findViewById(R.id.monthHeaderTextView)

        fun bind(month: String) {
            val monthParts = month.split(" ")
            if (monthParts.size == 2) {
                val monthName = monthParts[0]
                val year = monthParts[1]

                val formattedMonth = getItalianMonth(monthName) ?: monthName
                val monthText = "${formattedMonth.uppercase()} $year"
                monthHeaderTextView.text = monthText
            }
        }

        private fun getItalianMonth(monthName: String): String? {
            val englishMonths = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )

            val italianMonths = listOf(
                "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
                "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
            )

            val index = englishMonths.indexOf(monthName)
            return if (index >= 0 && index < italianMonths.size) {
                italianMonths[index]
            } else {
                null
            }
        }
    }

    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.detailDate)
        private val valueTextView: TextView = itemView.findViewById(R.id.detailValue)

        fun bind(data: DataModel) {
            // Bind the data to the views
            dateTextView.text = convertDateFormat(data.date)
            if (data.type == "accFerie" || data.type == "accPermesso") {
                valueTextView.text = "+ ${data.value}"
            } else {
                valueTextView.text = "${data.value}"
            }

            // Change the background color based on the data type
            val backgroundColor = if (data.type == "Ferie" || data.type == "accFerie") {
                if (data.type == "accFerie") {
                    ContextCompat.getColor(context, R.color.accumulated)
                } else {
                    ContextCompat.getColor(context, R.color.main)
                }
            } else {
                if (data.type == "accPermesso") {
                    ContextCompat.getColor(context, R.color.accumulated)
                } else {
                    ContextCompat.getColor(context, R.color.accent)
                }
            }
            itemView.setBackgroundColor(backgroundColor)

            // Change the text color based on the data type
            val textColor = if (data.type == "Ferie") {
                ContextCompat.getColor(context, R.color.white)
            } else {
                ContextCompat.getColor(context, R.color.white)
            }
            dateTextView.setTextColor(textColor)
            valueTextView.setTextColor(textColor)
        }

        private fun convertDateFormat(inputDate: String): String {
            // Check if the inputDate has the format "yyyy-MM" or "yyyy-MM-dd"
            val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val inputFormatWithDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            try {
                val date: Date?

                if (inputDate.length == "yyyy-MM".length) {
                    // If inputDate is in "yyyy-MM" format, append "-01" to represent the first day of the month
                    date = inputFormat.parse(inputDate + "-01")
                } else {
                    // If inputDate is in "yyyy-MM-dd" format, parse it directly
                    date = inputFormatWithDay.parse(inputDate)
                }

                if (date != null) {
                    // If the date parsing is successful, convert and return it in the desired format
                    return outputFormat.format(date)
                } else {
                    // Handle the case where parsing fails
                    return "Invalid Date" // You can choose to return some default value or handle the error as needed
                }
            } catch (e: ParseException) {
                // Handle the parsing exception
                e.printStackTrace()
                return "Invalid Date" // Return an error message or default value
            }
        }
    }
}

class SwipeToDeleteCallback(private val context: Context, private val adapter: DetailDataAdapter) : ItemTouchHelper.SimpleCallback(
    0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        // Draw a red background when swiping
        val background = ColorDrawable(Color.RED)
        background.setBounds(
            itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom
        )
        background.draw(c)

        // Call the default onChildDraw method to continue drawing the rest
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val deletedData = adapter.getDataAtPosition(position)
        // You can access recentlyDeletedItem and recentlyDeletedItemPosition here
        adapter.setRecentlyDeletedItem(adapter.getDataAtPosition(position))
        adapter.setRecentlyDeletedItemPosition(position)

        // Delete the data from the database using the id
        val databaseHelper = DatabaseHelper(context)
        //databaseHelper.insertAccData(13.33, 8.67, "2023-09-01")
        // Insert to test
        val deletedId = databaseHelper.deleteData(deletedData.id)
        val accData = databaseHelper.getAccDataById(deletedData.id)
        val accDeletedId = databaseHelper.deleteAccData(deletedData.id)
        databaseHelper.close()

        // Check if the deletion was successful
        if (deletedId != -1 || accDeletedId != -1) {
            // Remove the item from the local data list
            if (accDeletedId != 0) {
                adapter.setRecentlyDeletedAccItem(accData!!)
            }
            adapter.deleteItem(position)
        } else {
            // If deletion failed, you might want to show a message or handle the error
            Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
        }

        // Show a Snackbar with an Undo action
        val resources = context.resources
        // Retrieve a color by its resource ID
        val primaryColor = resources.getColor(R.color.main)
        val accentColor = resources.getColor(R.color.accent)
        val secondaryColor = resources.getColor(R.color.secondary)
        val snackbar = Snackbar.make(
            viewHolder.itemView,
            "Dato eliminato",
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(accentColor)
        snackbar.setTextColor(secondaryColor)
        snackbar.setActionTextColor(Color.YELLOW)
        // Set the anchor view for the Snackbar to appear above the ad
        /*val params = snackbar.view.layoutParams as CoordinatorLayout.LayoutParams
        params.anchorId = R.id.adView
        params.anchorGravity = Gravity.TOP
        params.gravity = Gravity.TOP
        snackbar.view.layoutParams = params*/

        snackbar.setAction("Annulla") {
            if (deletedId > 0) {
                adapter.undoDelete()
            } else {
                adapter.undoDeleteAcc()
            }
        }

        snackbar.show()
    }
}