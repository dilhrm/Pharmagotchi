package com.example.pharmagotchi.ui.pharmai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PharmAIViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "PharmAI Here"
    }
    val text: LiveData<String> = _text
}