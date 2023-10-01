package com.francisdeveloper.workrelaxquit.ui.kofi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.R

class KofiFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_kofi, container, false)

        // Find the ImageView by its ID
        val kofiImageView: ImageView = view.findViewById(R.id.kofiImageView)

        // Set an OnClickListener to open the Ko-fi page when the image is clicked
        kofiImageView.setOnClickListener {
            openKofiPage()
        }

        return view
    }

    private fun openKofiPage() {
        // Define your Ko-fi page URL here
        val kofiUrl = "https://ko-fi.com/yetanotherfrancis"

        // Open the Ko-fi page in a web browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kofiUrl))
        startActivity(intent)
    }
}
