package com.example.blastjargas.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MainViewModel: ViewModel() {
    var mutableLiveData = MutableLiveData<String>()

    fun setText(s: String) {
        mutableLiveData.value = s
    }

    fun getText(): MutableLiveData<String> {
        return mutableLiveData
    }
}