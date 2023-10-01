package com.francisdeveloper.workrelaxquit.ui.kofi

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.francisdeveloper.workrelaxquit.R

class KofiFragment : Fragment() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_kofi, container, false)

        // Find the WebView element in the fragment's layout
        val webView: WebView = view.findViewById(R.id.webViewKofi)

        // Enable JavaScript (optional, depends on your Ko-fi page)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Load your Ko-fi page in the WebView
        webView.loadUrl("https://ko-fi.com/yetanotherfrancis")

        // Handle WebView navigation within the WebView itself (e.g., links clicked)
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                return true
            }
        }

        return view
    }
}
