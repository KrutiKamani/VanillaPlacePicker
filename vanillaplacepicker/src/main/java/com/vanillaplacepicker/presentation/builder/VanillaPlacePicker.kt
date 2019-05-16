package com.vanillaplacepicker.presentation.builder

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.vanillaplacepicker.domain.common.Style
import com.vanillaplacepicker.extenstion.isRequiredField
import com.vanillaplacepicker.presentation.autocomplete.VanillaAutocompleteActivity
import com.vanillaplacepicker.presentation.map.VanillaMapActivity
import com.vanillaplacepicker.presentation.mapbox.autocomplete.VanillaMapBoxAutoCompleteActivity
import com.vanillaplacepicker.presentation.mapbox.map.VanillaMapBoxActivity
import com.vanillaplacepicker.utils.BundleUtils
import com.vanillaplacepicker.utils.KeyUtils
import com.vanillaplacepicker.utils.PickerLanguage
import wrap

class VanillaPlacePicker {

    companion object {
        val TAG = VanillaPlacePicker::class.java.simpleName
    }

    class Builder(private val context: Context) {
        private var isMapEnable: Boolean = false
        private var region: String? = null
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var radius: Int? = null
        private var language: String? = null
        private var minPrice: Int? = null
        private var maxPrice: Int? = null
        private var isOpenNow: Boolean? = null
        private var pageToken: String? = null
        private var types: String? = null
        private var tintColor: Int? = null
        private var minCharLimit: Int? = null
        private var zoneLocale: String? = null
        private var zoneRect: SearchZoneRect? = null
        private var zoneDefaultLocale = false
        private var enableSatelliteView = false
        private var googleTimeZoneEnabled = false
        private var mapStyleJSONResId: Int? = null
        private var mapStyleString: String? = null
        private var mapPinDrawable: Int? = null

        /**
         * To enable map view with place picker
         */
        fun enableMap(): Builder {
            this.isMapEnable = true
            return this
        }

        /**
         * When this parameter is used,
         * the country name is omitted from the resulting formatted_address for results in the specified region.
         */
        fun setRegion(region: String): Builder {
            this.region = region
            return this
        }

        /**
         * Request with default latitude & longitude for near by places
         */
        fun withLocation(latitude: Double, longitude: Double): Builder {
            this.latitude = latitude
            this.longitude = longitude
            return this
        }

        /**
         * Defines the distance (in meters) within which to bias place results.
         */
        fun setRadius(radius: Int): Builder {
            this.radius = radius
            return this
        }

        /**
         * Set default langauge for results
         */
        fun setLanguage(language: String): Builder {
            this.language = language
            return this
        }

        /**
         * Restricts results to only those places within the specified price level.
         * Valid values are in the range from 0 (most affordable) to 4 (most expensive), inclusive.
         */
        fun setMinPrice(minPrice: Int): Builder {
            this.minPrice = minPrice
            return this
        }

        /**
         * Restricts results to only those places within the specified price level.
         * Valid values are in the range from 0 (most affordable) to 4 (most expensive), inclusive.
         */
        fun setMaxPrice(maxPrice: Int): Builder {
            this.maxPrice = maxPrice
            return this
        }

        /**
         * Returns only those places that are open for business at the time the query is sent.
         */
        fun isOpenNow(openNow: Boolean): Builder {
            this.isOpenNow = openNow
            return this
        }

        fun setPageToken(pageToken: String): Builder {
            this.pageToken = pageToken
            return this
        }

        fun setTypes(types: String): Builder {
            this.types = types
            return this
        }

        /**
         * Apply Tint color to Back, Clear button of PlacePicker AutoComplete header UI
         */
        fun setTintColor(colorResourceId: Int): Builder {
            this.tintColor = colorResourceId
            return this
        }

        /**
         * Restrict user input limit to minimum char
         */
        fun setMinCharLimit(minCharLimit: Int): Builder {
            this.minCharLimit = minCharLimit
            return this
        }

        /**
         * To add zone locale
         */
        fun zoneLocale(zoneLocale: String): Builder {
            this.zoneLocale = zoneLocale
            return this
        }

        /**
         * To restrict request zone by rect
         */
        fun zoneRect(zoneRect: SearchZoneRect): Builder {
            this.zoneRect = zoneRect
            return this
        }

        fun zoneDefaultLocale(zoneDefaultLocale: Boolean): Builder {
            this.zoneDefaultLocale = zoneDefaultLocale
            return this
        }

        /**
         * To enable satellite view in map
         */
        fun enableSatelliteView(enableSatelliteView: Boolean): Builder {
            this.enableSatelliteView = enableSatelliteView
            return this
        }

        /**
         * Add a map with custom styling
         * With style options you can customize the presentation of the standard Google map styles,
         * changing the visual display of features like roads, parks, businesses, and other points of interest.
         * Add a resource containing a JSON style object (Use a raw resource)
         * */
        fun setMapStyle(jsonResourceIdMapStyle: Int): Builder {
            this.mapStyleJSONResId = jsonResourceIdMapStyle
            return this
        }

        /**
         * Add a map with custom styling
         * With style options you can customize the presentation of the standard Google map styles,
         * changing the visual display of features like roads, parks, businesses, and other points of interest.
         * Add a resource containing a JSON style object (Use a string resource)
         * */
        fun setMapStyle(stringMapStyle: String): Builder {
            this.mapStyleString = stringMapStyle
            return this
        }

        /**
         * Set custom Map Pin image
         * */
        fun setMapPinDrawable(mapPinDrawableResId: Int): Builder {
            this.mapPinDrawable = mapPinDrawableResId
            return this
        }

        /**
         * Set picker language
         */
        fun setPickerLanguage(pickerLanguage: PickerLanguage): Builder {
//            LocaleHelper.setLocale(context, pickerLanguage.value)
            ContextWrapper(context).wrap(pickerLanguage.value)
            return this
        }

        /**
         * Get Google Places API key
         */
        private fun getApiKey(): String {
            val metadataBundle: Bundle? = BundleUtils.getMetaData(context)
            if (metadataBundle != null) {
                return if (metadataBundle.getString("com.google.android.geo.API_KEY").isRequiredField())
                    metadataBundle.getString("com.google.android.geo.API_KEY")!!
                else {
                    Log.e(
                        TAG,
                        "Couldn't get Google API key from application meta data. Was it set in your AndroidManifest.xml?"
                    )
                    ""
                }
            } else {
                Log.e(
                    TAG,
                    "Couldn't get Google API key from application meta data. Was it set in your AndroidManifest.xml?"
                )
                return ""
            }
        }

        fun build(): Intent {
            val intent = if (isMapEnable) {
                Intent(context, VanillaMapActivity::class.java)
            } else {
                Intent(context, VanillaAutocompleteActivity::class.java)
            }

            intent.putExtra(KeyUtils.API_KEY, getApiKey())

            region?.let {
                intent.putExtra(KeyUtils.REGION, it)
            }
            latitude?.let {
                intent.putExtra(KeyUtils.LATITUDE, it)
            }
            longitude?.let {
                intent.putExtra(KeyUtils.LONGITUDE, it)
            }
            radius?.let {
                intent.putExtra(KeyUtils.RADIUS, it)
            }
            language?.let {
                intent.putExtra(KeyUtils.LANGUAGE, it)
            }
            minPrice?.let {
                intent.putExtra(KeyUtils.MIN_PRICE, it)
            }
            maxPrice?.let {
                intent.putExtra(KeyUtils.MAX_PRICE, it)
            }
            isOpenNow?.let {
                intent.putExtra(KeyUtils.OPEN_NOW, it)
            }
            pageToken?.let {
                intent.putExtra(KeyUtils.PAGE_TOKEN, it)
            }
            types?.let {
                intent.putExtra(KeyUtils.TYPES, it)
            }
            tintColor?.let {
                intent.putExtra(KeyUtils.TINT_COLOR, it)
            }
            minCharLimit?.let {
                intent.putExtra(KeyUtils.MIN_CHAR_LIMIT, it)
            }
            mapStyleJSONResId?.let {
                intent.putExtra(KeyUtils.MAP_STYLE_JSON_RES_ID, it)
            }
            mapStyleString?.let {
                intent.putExtra(KeyUtils.MAP_STYLE_STRING, it)
            }
            mapPinDrawable?.let {
                intent.putExtra(KeyUtils.MAP_PIN_DRAWABLE, it)
            }
            intent.putExtra(KeyUtils.ZONE_LOCALE, zoneLocale)
            intent.putExtra(KeyUtils.ZONE_RECT, zoneRect)
            intent.putExtra(KeyUtils.ZONE_DEFAULT_LOCALE, zoneDefaultLocale)
            intent.putExtra(KeyUtils.ENABLE_SATELLITE_VIEW, enableSatelliteView)
            intent.putExtra(KeyUtils.GOOGLE_TIMEZONE_ENABLED, googleTimeZoneEnabled)
            return intent
        }
    }

    class MapBoxBuilder(private val context: Context) {
        private var accessToken: String? = null
        private var isMapEnable: Boolean = false
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var minCharLimit: Int? = null
        private var limit: Int? = null
        private var style = Style.MAPBOX_STREETS
        private var styleUrl: String? = null
        private var language: String? = null
        private var country: String? = null
        private var types: String? = null
        private var proximityLongitude: Double? = null
        private var proximityLatitude: Double? = null

        /**
         * To enable map view with place picker
         */
        fun enableMap(): MapBoxBuilder {
            this.isMapEnable = true
            return this
        }

        /**
         * Request with default latitude & longitude for near by places
         */
        fun withLocation(latitude: Double, longitude: Double): MapBoxBuilder {
            this.latitude = latitude
            this.longitude = longitude
            return this
        }

        fun setMapStyle(style: Style): MapBoxBuilder {
            this.style = style
            return this
        }

        fun setMapStyle(styleUrl: String): MapBoxBuilder {
            this.styleUrl = styleUrl
            return this
        }

        /**
         * Restrict user input limit to minimum char
         */
        fun setMinCharLimit(minCharLimit: Int): MapBoxBuilder {
            this.minCharLimit = minCharLimit
            return this
        }

        fun setAccessToken(accessToken: String): MapBoxBuilder {
            this.accessToken = accessToken
            return this
        }

        /**
         * Specify the user’s language. This parameter controls the language of the text supplied in responses,
         * and also affects result scoring, with results matching the user’s query in the requested language
         * being preferred over results that match in another language. For example, an autocomplete query for
         * things that start with Frank might return Frankfurt as the first result with an English (en) language
         * parameter, but Frankreich (“France”) with a German (de) language parameter.
         */
        fun setLanguage(language: String): MapBoxBuilder {
            this.language = language
            return this
        }

        /**
         * Filter results to include only a subset (one or more) of the available feature types. Options are country,
         * region, postcode, district, place, locality, neighborhood, address, and poi. Multiple options can be
         * comma-separated. Note that poi.landmark is a deprecated type that, while still supported, returns the
         * same data as is returned using the poi type.
         */
        fun setTypes(types: String): MapBoxBuilder {
            this.types = types
            return this
        }

        /**
         * Specify the maximum number of results to return. The default is 5 and the maximum supported is 10.
         */
        fun setLimit(limit: Int): MapBoxBuilder {
            this.limit = limit
            return this
        }

        /**
         * Bias the response to favor results that are closer to this location, provided as two comma-separated
         * coordinates in longitude,latitude order.
         */
        fun setProximity(longitude: Double, latitude: Double): MapBoxBuilder {
            this.proximityLongitude = longitude
            this.proximityLatitude = latitude
            return this
        }

        /**
         * Limit results to one or more countries. Permitted values are ISO 3166 alpha 2 :
         * "https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2" country codes separated
         * by commas.
         */
        fun setCountry(country: String): MapBoxBuilder {
            this.country = country
            return this
        }

        fun build(): Intent {
            val intent = if (isMapEnable) {
                Intent(context, VanillaMapBoxActivity::class.java)
            } else {
                Intent(context, VanillaMapBoxAutoCompleteActivity::class.java)
            }

            if (accessToken.isRequiredField()) {
                intent.putExtra(KeyUtils.MAPBOX_ACCESS_TOKEN, accessToken)
            } else {
                throw RuntimeException("You must provide a Mapbox API access token for Mapbox tile sources.")
            }

            latitude?.let {
                intent.putExtra(KeyUtils.LATITUDE, it)
            }

            longitude?.let {
                intent.putExtra(KeyUtils.LONGITUDE, it)
            }

            minCharLimit?.let {
                intent.putExtra(KeyUtils.MIN_CHAR_LIMIT, it)
            }

            language?.let {
                intent.putExtra(KeyUtils.LANGUAGE, it)
            }

            country?.let {
                intent.putExtra(KeyUtils.COUNTRY, it)
            }

            val mapStyle = findStyle(style)
            intent.putExtra(KeyUtils.MAPBOX_MAP_STYLE, mapStyle)

            styleUrl?.let {
                intent.putExtra(KeyUtils.MAPBOX_MAP_STYLE_URL, it)
            }

            limit?.let {
                intent.putExtra((KeyUtils.LIMIT), it)
            }

            types?.let {
                intent.putExtra(KeyUtils.TYPES, it)
            }

            if (proximityLongitude != null && proximityLongitude != null) {
                intent.putExtra((KeyUtils.PROXIMITY), "$proximityLongitude,$proximityLatitude")
            }

            return intent
        }

        private fun findStyle(style: Style): String {
            when (style) {
                Style.MAPBOX_STREETS -> {
                    return com.mapbox.mapboxsdk.maps.Style.MAPBOX_STREETS
                }
                Style.SATELLITE -> {
                    return com.mapbox.mapboxsdk.maps.Style.SATELLITE
                }
                Style.DARK -> {
                    return com.mapbox.mapboxsdk.maps.Style.DARK
                }
                Style.OUTDOORS -> {
                    return com.mapbox.mapboxsdk.maps.Style.OUTDOORS
                }
                Style.LIGHT -> {
                    return com.mapbox.mapboxsdk.maps.Style.LIGHT
                }
            }
        }
    }

}