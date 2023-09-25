package com.francisdeveloper.workrelaxquit.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.francisdeveloper.workrelaxquit.R
import com.francisdeveloper.workrelaxquit.databinding.FragmentHomeBinding
import com.francisdeveloper.workrelaxquit.ui.gestore.DatabaseHelper
import com.francisdeveloper.workrelaxquit.ui.gestore.getCurrentDate
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private lateinit var popupWindow: PopupWindow
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var dataList: MutableList<DataModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Determine the app's current theme (light or dark)
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // Initialize data list and adapter
        dataList = mutableListOf()
        // Set up database helper
        databaseHelper = DatabaseHelper(requireContext())

        // Set initial button state based on input validation
        binding.submitButton.isEnabled = isInputValid()

        // Set up listeners to update button state when the input fields change
        binding.ferieHours.addTextChangedListener(textWatcher)
        binding.permessiHours.addTextChangedListener(textWatcher)
        binding.ferieYear.addTextChangedListener(textWatcher)
        binding.permessiYear.addTextChangedListener(textWatcher)

        val adView = binding.adView
        MobileAds.initialize(requireContext())
        adView.loadAd(AdRequest.Builder().build())

        val infoIcon = binding.firstInfoIcon
        infoIcon.setOnClickListener {
            showInfoDialog()
        }

        val secondInfoIcon = binding.secondInfoIcon
        secondInfoIcon.setOnClickListener {
            showSecondInfoDialog()
        }

        // Set initial state of the reset button
        updateResetButtonState()

        binding.submitButton.setOnClickListener {
            // val yearsText = binding.yearsText.text.toString()
            // val yearsInt = yearsText.toInt()
            val ferieHours = binding.ferieHours.text.toString()
            val floatFerieHours = ferieHours.toDouble()
            val permessiHours = binding.permessiHours.text.toString()
            val floatPermessiHours = permessiHours.toDouble()
            val yearFerieDays = binding.ferieYear.text.toString()
            val intYearFerieDays = yearFerieDays.toInt()
            val yearPermessiHours = binding.permessiYear.text.toString()
            val floatYearPermessiHours = yearPermessiHours.toDouble()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1) // Set the day to the 1st day of the month
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val newData = DataModel(floatFerieHours, floatPermessiHours, intYearFerieDays, floatYearPermessiHours, dateFormat.format(calendar.time).toString())
            dataList.add(newData)
            // Save data in SQLite database
            val insertedId = databaseHelper.insertInitialData(floatFerieHours, floatPermessiHours, intYearFerieDays, floatYearPermessiHours, dateFormat.format(calendar.time).toString())
            if (insertedId != -1L) {
                newData.id = insertedId.toInt()
            }

            // Clear the text
            binding.ferieHours.setText("")
            binding.permessiHours.setText("")
            binding.ferieYear.setText("")
            binding.permessiYear.setText("")

            val accFerie = ((intYearFerieDays.toDouble() * 8) / 12).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val accPermessi = (floatYearPermessiHours / 12).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val df = DecimalFormat("#.##")

            // Update the reset button state after adding data
            updateResetButtonState()

            // Show the popup window when data is submitted
            showPopup("I dati sono stati inseriti!\n\n" +
                      "Ogni primo del mese riceverai una notifica per l'aggiornamento delle ore di ferie e di permesso.\n" +
                      "Ogni mese maturerai ${df.format(accFerie)} ore di ferie e ${df.format(accPermessi)} ore di " +
                      "permesso in base ai dati che hai inserito!")
        }

        binding.resetButton.setOnClickListener {
            // Show the confirmation popup when the 'resetButton' is clicked
            showConfirmationPopup()

            // databaseHelper.deleteAllData()
            // databaseHelper.deleteAllAccumulated()
            // databaseHelper.deleteInsertedData()

            // Update the reset button state after deleting data
            // updateResetButtonState()

            // Show the popup window when data is reset
            // showPopup("I dati sono stati rimossi!")
        }
    }

    private fun isInputValid(): Boolean {
        // val yearsText = binding.yearsText.text.toString()
        val ferieHours = binding.ferieHours.text.toString()
        val permessiHours = binding.permessiHours.text.toString()
        val yearFerieDays = binding.ferieYear.text.toString()
        val yearPermessiHours = binding.permessiYear.text.toString()

        // Check if any of the fields are empty
        if (ferieHours.isBlank() || permessiHours.isBlank() || yearFerieDays.isBlank() || yearPermessiHours.isBlank()) {
            return false
        }

        // Check if the 'InitialData' table is empty
        val databaseHelper = DatabaseHelper(requireContext())
        val firstRowData = databaseHelper.getFirstRow()
        return firstRowData == null
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not used, leave it empty
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Update the button state whenever the text changes
            binding.submitButton.isEnabled = isInputValid()
        }

        override fun afterTextChanged(s: Editable?) {
            // Not used, leave it empty
        }
    }

    private fun showInfoDialog() {
        // You can show a dialog or a tooltip here with additional information.
        // For example, you can use AlertDialog or PopupWindow.
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Ferie da contratto")
            .setMessage("I giorni di ferie in un anno dipendono dal contratto e dagli anni di anzianità del dipendente. " +
                    "Per il contratto metalmeccanico di terzo livello per esempio sono previsti 20 giorni.\n" +
                    "Puoi calcolare quanti giorni di ferie hai l'anno prendendo le tue ultime due buste paga:\n" +
                    "(ferie ultima busta paga - ferie penultima busta paga), moltiplica questo risultato per 12 e poi dividi il tutto per 8.\n" +
                    "Ricorda di prendere le ferie al netto di eventuali altre ferie che hai consumato!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun showSecondInfoDialog() {
        // You can show a dialog or a tooltip here with additional information.
        // For example, you can use AlertDialog or PopupWindow.
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Permessi da contratto")
            .setMessage("Le ore di permesso previste in genere sono 104, ma per sapere il valore con sicurezza puoi prendere " +
                    "le tue ultime due buste paga e fare il seguente calcolo:\n" +
                    "(permessi ultima busta paga - permessi penultima busta paga), moltiplica questo risultato per 12 e arrotonda all'intero più vicino.\n" +
                    "Ricorda di prendere i permessi al netto di eventuali altro permessi che hai consumato!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun showPopup(message: String) {
        // Create the Dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.popup_window_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Find the views in the custom layout
        val popupText = dialog.findViewById<TextView>(R.id.popupText)
        val popupCloseButton = dialog.findViewById<Button>(R.id.popupCloseButton)

        // Set the message
        popupText.text = message

        // Show the Dialog
        dialog.show()

        // Dismiss the Dialog after a certain delay (e.g., 2000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 30000)

        // Close the Dialog when the close button is clicked
        popupCloseButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showConfirmationPopup() {
        // Inflate the custom layout for the confirmation popup
        val popupView = layoutInflater.inflate(R.layout.popup_confirmation, null)

        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(popupView)
            .create()

        // Find the buttons in the custom layout
        val proceedButton = popupView.findViewById<Button>(R.id.proceedButton)
        val cancelButton = popupView.findViewById<Button>(R.id.cancelButton)

        // Set a click listener for the "Proceed" button
        proceedButton.setOnClickListener {
            // Delete data when the user confirms
            databaseHelper.deleteAllData()
            databaseHelper.deleteAllAccumulated()
            databaseHelper.deleteInsertedData()

            // Update the reset button state after deleting data
            updateResetButtonState()

            // Dismiss the confirmation popup
            alertDialog.dismiss()

            // Show the popup window when data is reset
            showPopup("I dati sono stati rimossi!")
        }

        // Set a click listener for the "Cancel" button
        cancelButton.setOnClickListener {
            // Close the confirmation popup without deleting data
            alertDialog.dismiss()
        }

        // Show the confirmation popup
        alertDialog.show()
    }

    private fun updateResetButtonState() {
        val databaseHelper = DatabaseHelper(requireContext())
        val initialDataIsEmpty = databaseHelper.getFirstRow() == null
        binding.resetButton.isEnabled = !initialDataIsEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class DataModel(val ferie: Double, val permessi: Double, val giorniFerie: Int, val permessiHours: Double, val initialDate: String, var id: Int = 0)
data class AccDataModel(val accFerie: Double, val accPermessi: Double, val date: String, var id: Int = 0)