package com.vanillaplacepicker.presentation.mapbox.map

import androidx.lifecycle.MutableLiveData
import com.mapbox.mapboxsdk.geometry.LatLng
import com.vanillaplacepicker.presentation.common.VanillaBaseViewModel
import com.vanillaplacepicker.utils.SharedPrefs

class VanillaMapBoxViewModel(private val sharedPrefs: SharedPrefs) : VanillaBaseViewModel() {
    var latLngLiveData = MutableLiveData<LatLng>()

    fun fetchSavedLocation() {
        latLngLiveData.value = (LatLng(
            sharedPrefs.deviceLatitude.toDouble(),
            sharedPrefs.deviceLongitude.toDouble()
        ))
    }

    fun saveLatLngToSharedPref(latitude: Double, longitude: Double) {
        sharedPrefs.deviceLatitude = latitude.toFloat()
        sharedPrefs.deviceLongitude = longitude.toFloat()
    }
}