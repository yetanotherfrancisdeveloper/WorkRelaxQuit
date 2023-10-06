package com.francisdeveloper.workrelaxquit.ui.contactme

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.databinding.FragmentContactBinding

class ContactFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSendEmail.setOnClickListener {
            // Get the values from the EditText fields
            val recipientEmail = binding.editTextRecipient.text.toString()
            val subject = binding.editTextSubject.text.toString()
            val body = binding.editTextBody.text.toString()

            // Create an email intent
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail?subject=$subject&body=$body")
            }

            // Check if there's an email app available to handle the intent
            try {
                startActivity(Intent.createChooser(emailIntent, "Scegli un'app per inviare la mail"))
            } catch (e: ActivityNotFoundException) {
                //Toast.makeText(this, "Non è stata trovata un'app per inviare una mail.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
