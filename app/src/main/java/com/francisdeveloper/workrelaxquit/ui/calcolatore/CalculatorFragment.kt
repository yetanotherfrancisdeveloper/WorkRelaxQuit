package com.francisdeveloper.workrelaxquit.ui.calcolatore

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.MobileAds
import com.francisdeveloper.workrelaxquit.databinding.FragmentCalcolatoreBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalculatorFragment : Fragment() {
    private var _binding: FragmentCalcolatoreBinding? = null
    private val binding get() = _binding!!
    private var ferieMonthly = 0.0
    private var availableFerie = 0.0
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalcolatoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Number input fields
        val number1EditText = binding.ferie
        val number2EditText = binding.toUseFerie
        val number3EditText = binding.holidayFerie

        setupDecimalSeparatorConversion(number1EditText)

        databaseHelper = DatabaseHelper(requireContext())

        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()
        // Get all used hours of ferie
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()

        val df = DecimalFormat("#.##")

        if (firstRowData != null) {
            val df = DecimalFormat("#.##")
            // Access the data from the first row
            val ferie = firstRowData.ferie
            val permessi = firstRowData.permessi
            val giorniFerie = firstRowData.giorniFerie
            number1EditText.setText(df.format(ferie))

            availableFerie = ferie - sumUsedFerie + ferieAccumulated
            ferieMonthly = ((giorniFerie * 8) / 12).toDouble()
            number1EditText.setText(df.format(availableFerie).toString())
        } else {
            ferieMonthly = 13.33
        }

        if (availableFerie != 0.0) {
            number1EditText.setText(df.format(availableFerie).toString())
        }

        // Result display
        //val resultTextView = binding.resultTextView

        // Set initial button state based on input validation
        binding.calculateButton.isEnabled = isInputValid()

        // Set up listeners to update button state when the input fields change
        binding.ferie.addTextChangedListener(textWatcher)
        binding.toUseFerie.addTextChangedListener(textWatcher)
        binding.holidayFerie.addTextChangedListener(textWatcher)

        /*val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())*/

        binding.calculateButton.setOnClickListener {
            val ferieAvailable = number1EditText.text.toString().toDouble()
            val ferieToSpend = number2EditText.text.toString().toDouble() * 8.0
            val ferieToUse = number3EditText.text.toString().toDouble()
            val ferieToMature = abs(ferieAvailable - (ferieToUse * 8) - ferieToSpend)

            var monthsForFerie = abs(ferieToMature / ferieMonthly).toInt()
            var currentDate = LocalDate.now()
            var ferieDate = currentDate.plusMonths(monthsForFerie.toLong())
            var ferieMonth = ferieDate.month
            var ferieYear = ferieDate.year
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ITALIAN)
            var monthInItalian = monthFormatter.format(ferieMonth)
            var ferieResidual = ferieMonthly * monthsForFerie - ferieToMature

            if (ferieResidual < 0) {
                monthsForFerie = abs((ferieToMature) / ferieMonthly).toInt() + 1
                currentDate = LocalDate.now()
                ferieDate = currentDate.plusMonths(monthsForFerie.toLong())
                ferieMonth = ferieDate.month
                ferieYear = ferieDate.year
                monthInItalian = monthFormatter.format(ferieMonth)
                ferieResidual = ferieMonthly * monthsForFerie - ferieToMature
            }

            val resultText = "Potrai andare in ferie: $monthInItalian $ferieYear"

            val collapsedText = binding.collapsedText
            val expandedText = binding.expandedText
            val lineSeparator = binding.lineSeparator
            val collapsibleCardView = binding.collapsibleCardView
            collapsibleCardView.visibility = View.VISIBLE

            collapsedText.text = resultText
            expandedText.text = "- Ore di ferie necessarie in più: ${ferieToMature.toInt()}\n" +
                    "- Mesi necessari per accumularle: $monthsForFerie\n" +
                    "- Ore di ferie dopo la vacanza: ${ferieResidual.toInt()}"

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

    private fun isInputValid(): Boolean {
        val ralText = binding.ferie.text.toString()
        val hoursText = binding.toUseFerie.text.toString()
        val selectedOptionText = binding.holidayFerie.text.toString()

        return ralText.isNotBlank() && hoursText.isNotBlank() && selectedOptionText.isNotBlank()
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

    private fun setupDecimalSeparatorConversion(editText: EditText) {
        val userDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used, leave it empty
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Detect if the user's input uses a different decimal separator
                val userInput = editText.text.toString()
                if (userInput.contains(',')) {
                    // Replace the user's decimal separator with a dot
                    val convertedInput = userInput.replace(',', '.')
                    // Update the EditText with the converted input
                    editText.setText(convertedInput)
                    // Set the cursor position to the end
                    editText.setSelection(convertedInput.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used, leave it empty
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()

        val number1EditText = binding.ferie
        setupDecimalSeparatorConversion(number1EditText)
        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()
        // Get all used hours of ferie
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()

        val df = DecimalFormat("#.##")

        if (firstRowData != null) {
            val df = DecimalFormat("#.##")
            // Access the data from the first row
            val ferie = firstRowData.ferie
            val permessi = firstRowData.permessi
            val giorniFerie = firstRowData.giorniFerie
            number1EditText.setText(df.format(ferie))

            availableFerie = ferie - sumUsedFerie + ferieAccumulated
            ferieMonthly = ((giorniFerie * 8) / 12).toDouble()
            number1EditText.setText(df.format(availableFerie).toString())
        } else {
            ferieMonthly = 13.33
        }

        if (availableFerie != 0.0) {
            number1EditText.setText(df.format(availableFerie).toString())
        }
    }
}