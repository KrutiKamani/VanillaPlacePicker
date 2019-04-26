package com.vanillaplacepicker.data

import com.google.gson.annotations.SerializedName

class MapBoxGeoCoderAddressResponse {

    @SerializedName("features")
    var features: ArrayList<Features>? = null

    @SerializedName("query")
    var query: Array<String>? = null

    @SerializedName("type")
    var type: String? =null

    @SerializedName("attribution")
    var attribution: String? =null

    inner class Features {

        @SerializedName("place_name")
        var place_name: String? = null

        @SerializedName("id")
        var id: String? = null

        @SerializedName("text")
        var text: String? = null

        @SerializedName("type")
        var type: String? = null

        @SerializedName("relevance")
        var relevance: String? = null

        @SerializedName("place_type")
        var place_type: Array<String>? = null

        @SerializedName("bbox")
        var bbox: Array<Double>? = null

        @SerializedName("center")
        var center: Array<Double>? = null

        @SerializedName("context")
        var context: ArrayList<Context>? = null

        @SerializedName("geometry")
        var geometry: Geometry? = null

        @SerializedName("properties")
        var properties: Properties? = null

        inner class Geometry {

            @SerializedName("coordinates")
            var coordinates: Array<Double>? =null

            @SerializedName("type")
            var type: String? = null
        }

        inner class Properties {

        }

        inner class Context {

            @SerializedName("id")
            var id: String? = null

            @SerializedName("text")
            var text: String? = null

            @SerializedName("wikidata")
            var wikidata: String? = null
        }
    }
}