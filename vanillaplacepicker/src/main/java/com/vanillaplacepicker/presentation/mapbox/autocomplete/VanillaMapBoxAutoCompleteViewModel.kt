package com.vanillaplacepicker.presentation.mapbox.autocomplete

import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.vanillaplacepicker.data.MapBoxGeoCoderAddressResponse
import com.vanillaplacepicker.domain.common.Resource
import com.vanillaplacepicker.domain.common.Status
import com.vanillaplacepicker.domain.repository.WebApiClient
import com.vanillaplacepicker.presentation.common.VanillaBaseViewModel
import com.vanillaplacepicker.utils.KeyUtils
import com.vanillaplacepicker.utils.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class VanillaMapBoxAutoCompleteViewModel : VanillaBaseViewModel() {
    val autoCompleteLiveData = MutableLiveData<Resource<MapBoxGeoCoderAddressResponse>>()
    val showClearButtonLiveData = MutableLiveData<Boolean>()
    private val autoCompletePublishSubject = PublishRelay.create<String>()
    private var minCharLimit: Int? = null
    private var hashMap = HashMap<String, String>()

    fun configureAutoComplete(minCharLimit: Int, hashMap: HashMap<String, String>) {
        this.minCharLimit = minCharLimit
        this.hashMap = hashMap

        autoCompletePublishSubject
            .debounce(KeyUtils.DEBOUNCE_INTERVAL, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .filter { it.trim().length >= minCharLimit }
            .flatMap {
                callMApBoxGeoCodingAPI(hashMap, it)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ result ->
                onAutoCompleteResultReceived(result)
            }, { t: Throwable? ->
                t?.let {
                    autoCompleteLiveData.value = Resource(Status.ERROR, it)
                }
                Logger.e("Failed to get search results", t)
            }).collect()
    }

    private fun onAutoCompleteResultReceived(result: MapBoxGeoCoderAddressResponse) {
        if (result.features!!.size > 0) {
            autoCompleteLiveData.value = Resource(Status.SUCCESS, result)
        } else {
            autoCompleteLiveData.value = Resource(Status.ERROR, result)
        }
    }

    fun onInputStateChanged(query: String, minCharLimit: Int) {
        showClearButtonLiveData.value = query.isNotEmpty() // query.length > 0 with empty check
        if (query.length < minCharLimit) {
            autoCompleteLiveData.value = Resource(Status.ERROR)
        }
        autoCompletePublishSubject.accept(query.trim())
    }

/*    private fun callMapBoxGeoCodingAPI(result: String, hashMap: HashMap<String, String>) {
        autoCompleteLiveData.value = Resource(Status.LOADING)
        val client = MapboxGeocoding.builder()
            .accessToken(hashMap[KeyUtils.MAPBOX_ACCESS_TOKEN]!!)
            .query(result)
            .languages("en")
            .build()

        client.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                autoCompleteLiveData.value = Resource(Status.ERROR, t)
            }

            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.isSuccessful && response.body() != null ) {
                    autoCompleteLiveData.value = Resource(Status.SUCCESS)
                    onAutoCompleteResultReceived(response.body()!!)
                } else {
                    autoCompleteLiveData.value = Resource(Status.ERROR)
                }
            }
        })
    }*/

    private fun callMApBoxGeoCodingAPI(hashMap: HashMap<String, String>, query: String): Observable<MapBoxGeoCoderAddressResponse> {
        return WebApiClient.webApiMapBox.searchMapBoxPlace(query, hashMap)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

}