package com.vanillaplacepicker.domain.repository

import com.vanillaplacepicker.data.MapBoxGeoCoderAddressResponse
import com.vanillaplacepicker.data.SearchAddressResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface WebApi {

    @GET("textsearch/json")
    fun searchPlace(@QueryMap param: HashMap<String, String>): Observable<SearchAddressResponse>

    @GET("mapbox.places/{query}.json")
    fun searchMapBoxPlace(@Path("query") search_text: String, @QueryMap param: HashMap<String, String>):
            Observable<MapBoxGeoCoderAddressResponse>
}