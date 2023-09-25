package com.francisdeveloper.workrelaxquit.ui.valoreferie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ValoreFerieViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Valore Ferie Fragment"
    }
    val text: LiveData<String> = _text
}