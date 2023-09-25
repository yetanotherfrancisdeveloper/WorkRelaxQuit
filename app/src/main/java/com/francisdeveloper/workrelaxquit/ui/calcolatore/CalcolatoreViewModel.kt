package com.francisdeveloper.workrelaxquit.ui.calcolatore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalcolatoreViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is calcolatore Fragment"
    }
    val text: LiveData<String> = _text
}