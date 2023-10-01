package com.francisdeveloper.workrelaxquit.ui.valoreferie

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.MobileAds
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentValoreFerieBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols


class ValoreFerieFragment : Fragment() {
    private var _binding: FragmentValoreFerieBinding? = null
    private val binding get() = _binding!!
    private var availableFerie = 0.0
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var dropdownMenu: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentValoreFerieBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RAL
        val ral = binding.ralText
        // Number of ferie hours
        val ferieHours = binding.hoursText

        setupDecimalSeparatorConversion(ral)
        setupDecimalSeparatorConversion(ferieHours)

        databaseHelper = DatabaseHelper(requireContext())

        /*val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())*/

        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()
        // Get all used hours of ferie
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()

        val df = DecimalFormat("#.##")
        if (firstRowData != null) {
            // Access the data from the first row
            val ferie = firstRowData.ferie
            availableFerie = ferie - sumUsedFerie + ferieAccumulated
            ferieHours.setText(df.format(availableFerie).toString())
        }

        if (availableFerie != 0.0) {
            ferieHours.setText(df.format(availableFerie).toString())
        }

        // Result display
        // val resultTextView = binding.resultTextView
        if (availableFerie != 0.0) {
            ferieHours.setText(df.format(availableFerie).toString())
        }

        // Set initial button state based on input validation
        binding.calculateButton.isEnabled = isInputValid()

        // Set up listeners to update button state when the input fields change
        binding.ralText.addTextChangedListener(textWatcher)
        binding.hoursText.addTextChangedListener(textWatcher)

        dropdownMenu = binding.dropdownMenu
        val options = arrayOf("5", "6")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        dropdownMenu.adapter = adapter

        binding.calculateButton.setOnClickListener {
            // Number input fields
            val ralDouble = ral.text.toString().toDouble()
            val ralImponibile = ralDouble - (ralDouble * 0.09)
            val IRPEFTax = calculateIRPEFTax(ralImponibile)
            val ran = ralImponibile - IRPEFTax
            val selectedOption = dropdownMenu.selectedItem as String

            val ferieDouble = ferieHours.text.toString().toDouble()
            val rmn = ran / 12

            val rdn = if (selectedOption == "5") {
                rmn / 22
            } else {
                rmn / 26
            }

            val rhn = rdn / 8
            val ferieValue = rhn * ferieDouble
            val resultRounded = ferieValue.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val ralImponibileRounded = ralImponibile.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val IRPEFTaxRounded = IRPEFTax.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val rhnRounded = rhn.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val resultText = "Le tue ferie valgono: \u20AC$resultRounded"

            val collapsedText = binding.collapsedText
            val expandedText = binding.expandedText
            val lineSeparator = binding.lineSeparator
            val collapsibleCardView = binding.collapsibleCardView
            collapsibleCardView.visibility = View.VISIBLE

            collapsedText.text = resultText
            expandedText.text = "- Valore di un'ora di ferie: \u20AC$rhnRounded"
                                //"- RAL imponibile: $ralImponibileRounded\n" +
                                //"- IRPEF: $IRPEFTaxRounded\n" +


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
        val ralText = binding.ralText.text.toString()
        val hoursText = binding.hoursText.text.toString()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()

        // Get first row of InitialData
        val firstRowData = databaseHelper.getFirstRow()
        // Get all used hours of ferie
        val sumUsedFerie = databaseHelper.getSumOfColumnValues("Ferie")
        // Get ferie hours accumulated
        val ferieAccumulated = databaseHelper.getSumOfAccumulatedFerie()
        val ferieHours = binding.hoursText
        setupDecimalSeparatorConversion(ferieHours)

        val df = DecimalFormat("#.##")
        if (firstRowData != null) {
            // Access the data from the first row
            val ferie = firstRowData.ferie
            availableFerie = ferie - sumUsedFerie + ferieAccumulated
            ferieHours.setText(df.format(availableFerie).toString())
        }

        if (availableFerie != 0.0) {
            ferieHours.setText(df.format(availableFerie).toString())
        }
    }
}