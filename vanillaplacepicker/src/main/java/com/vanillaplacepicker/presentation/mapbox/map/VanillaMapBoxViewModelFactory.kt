package com.vanillaplacepicker.presentation.mapbox.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vanillaplacepicker.utils.SharedPrefs

class VanillaMapBoxViewModelFactory(private val sharedPrefs: SharedPrefs) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.cast(VanillaMapBoxViewModel(sharedPrefs)) as T
    }
}