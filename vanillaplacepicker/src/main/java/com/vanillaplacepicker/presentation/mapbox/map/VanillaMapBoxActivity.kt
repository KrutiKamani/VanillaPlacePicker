package com.vanillaplacepicker.presentation.mapbox.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.vanillaplacepicker.R
import com.vanillaplacepicker.data.common.AddressMapperMapBoxMap
import com.vanillaplacepicker.domain.common.SafeObserver
import com.vanillaplacepicker.extenstion.*
import com.vanillaplacepicker.presentation.common.VanillaBaseViewModelActivity
import com.vanillaplacepicker.presentation.mapbox.autocomplete.VanillaMapBoxAutoCompleteActivity
import com.vanillaplacepicker.utils.KeyUtils
import com.vanillaplacepicker.utils.Logger
import com.vanillaplacepicker.utils.SharedPrefs
import kotlinx.android.synthetic.main.activity_mapbox_map.*
import kotlinx.android.synthetic.main.toolbar.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

class VanillaMapBoxActivity : VanillaBaseViewModelActivity<VanillaMapBoxViewModel>(), OnMapReadyCallback,
    View.OnClickListener, PermissionsListener {

    private val TAG = VanillaMapBoxActivity::class.java.simpleName

    private var mapBoxMap: MapboxMap? = null
    private var locationEngine: LocationEngine? = null

    private var isRequestedWithLocation = false
    private var accessToken: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var midLatLng: LatLng? = null
    private var mapStyle: String? = null
    private var mapStyleUrl: String? = null
    private var minCharLimit: Int = 3
    private var limit: Int? = null
    private var language: String? = null
    private var country: String? = null
    private var proximity: String? = null
    private var types: String? = null

    private var DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
    private var DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private val sharedPrefs by lazy { SharedPrefs(this) }

    // Variables needed to listen to location updates
    private var callback = LocationCallback()
    var activityWeakReference: WeakReference<VanillaMapBoxActivity> = WeakReference<VanillaMapBoxActivity>(this)

    private var selectedPlace: CarmenFeature? = null

    override fun buildViewModel(): VanillaMapBoxViewModel {
        return ViewModelProviders.of(
            this,
            VanillaMapBoxViewModelFactory(sharedPrefs)
        )[VanillaMapBoxViewModel::class.java]
    }

    override fun getContentResource(): Int {
        getBundle()
        Mapbox.getInstance(this, accessToken)
        return R.layout.activity_mapbox_map
    }

    override fun initViews() {
        super.initViews()
        supportActionBar?.hide()
        tvAddress.isSelected = true
        ivBack.setOnClickListener(this)
        ivDone.setOnClickListener(this)
        tvAddress.setOnClickListener(this)
        iv_my_location.setOnClickListener(this)

        mapView?.getMapAsync(this)
    }

    private fun getBundle() {
        if (hasExtra(KeyUtils.MAPBOX_ACCESS_TOKEN)) {
            accessToken = intent.getStringExtra(KeyUtils.MAPBOX_ACCESS_TOKEN)
        }

        if (hasExtra(KeyUtils.LATITUDE)) {
            isRequestedWithLocation = true
            latitude = intent.getDoubleExtra(KeyUtils.LATITUDE, 0.0)
        }

        if (hasExtra(KeyUtils.LONGITUDE)) {
            longitude = intent.getDoubleExtra(KeyUtils.LONGITUDE, 0.0)
        }

        if (hasExtra(KeyUtils.MAPBOX_MAP_STYLE)) {
            mapStyle = intent.getStringExtra(KeyUtils.MAPBOX_MAP_STYLE)
        }

        if (hasExtra(KeyUtils.MAPBOX_MAP_STYLE_URL)) {
            mapStyleUrl = intent.getStringExtra(KeyUtils.MAPBOX_MAP_STYLE_URL)
        }

        if (hasExtra(KeyUtils.LANGUAGE)) {
            language = intent.getStringExtra(KeyUtils.LANGUAGE)
        }

        if (hasExtra(KeyUtils.COUNTRY)) {
            country = intent.getStringExtra(KeyUtils.COUNTRY)
        }

        if (hasExtra(KeyUtils.MIN_CHAR_LIMIT)) {
            minCharLimit = intent.getIntExtra(KeyUtils.MIN_CHAR_LIMIT, 3)
        }

        if (hasExtra(KeyUtils.LIMIT)) {
            limit = intent.getIntExtra(KeyUtils.LIMIT, 5)
        }

        if (hasExtra(KeyUtils.PROXIMITY)) {
            proximity = intent.getStringExtra(KeyUtils.PROXIMITY)
        }

        if (hasExtra(KeyUtils.TYPES)) {
            types = intent.getStringExtra(KeyUtils.TYPES)
        }
    }

    override fun initLiveDataObservers() {
        super.initLiveDataObservers()
        viewModel.latLngLiveData.observe(this, SafeObserver(this::moveCameraToPosition))
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBoxMap = mapboxMap

        mapStyleUrl?.let {
            mapStyle = mapStyleUrl
        }

        mapBoxMap!!.setStyle(mapStyle) {

            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
            if (!isRequestedWithLocation) {
                iv_my_location.visibility = View.VISIBLE
                enableLocationComponent(it)
            } else {
                moveCameraToPosition(LatLng(latitude, longitude))
            }
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            val locationComponent = mapBoxMap?.locationComponent

            // Set the LocationComponent activation options
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .useDefaultLocationEngine(false)
                    .build()

            // Activate with the LocationComponentActivationOptions object
            locationComponent?.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent?.isLocationComponentEnabled = false

            // Set the component's camera mode
//            locationComponent?.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
//            locationComponent?.renderMode = RenderMode.COMPASS

            startLocationUpdates()
        } else {
            requestForLocationPermission()
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)

        locationEngine!!.getLastLocation(callback)
    }

    private fun requestForLocationPermission() {
        permissionsManager = PermissionsManager(this)
        permissionsManager.requestLocationPermissions(this)
    }

    private fun moveCameraToPosition(latLng: LatLng?) {

        val cameraUpdateDefaultLocation = CameraUpdateFactory.newLatLngZoom(
            latLng!!,
            if (latLng.latitude == 0.0) 0.0 else KeyUtils.DEFAULT_ZOOM_LEVEL.toDouble()
        )
        mapBoxMap?.animateCamera(
            cameraUpdateDefaultLocation,
            KeyUtils.GOOGLE_MAP_CAMERA_ANIMATE_DURATION,
            null
        )

//        mapBoxMap?.setPadding(0, 256, 0, 0)
        mapBoxMap?.addOnCameraMoveListener {
            tvAddress.text = getString(R.string.searching)
            ivDone.hideView()
        }

        mapBoxMap?.addOnCameraIdleListener {
            val newLatLng = this@VanillaMapBoxActivity.mapBoxMap?.cameraPosition?.target
            newLatLng?.let {
                midLatLng?.let { midLatLng ->
                    if (it.latitude == midLatLng.latitude && it.longitude == midLatLng.longitude) {
                        return@addOnCameraIdleListener
                    }
                }
                midLatLng = this@VanillaMapBoxActivity.mapBoxMap?.cameraPosition?.target
                midLatLng?.let { centerPoint ->
                    getAddressFromMapBoxGeoCoder(centerPoint)
                }
            }
        }
    }

    private fun getAddressFromMapBoxGeoCoder(centerPoint: LatLng) {
        val client = MapboxGeocoding.builder()
            .accessToken(accessToken!!)
            .query(Point.fromLngLat(centerPoint.longitude, centerPoint.latitude))
            .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
            .build()

        client.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                Log.d(TAG, "GeoCoding failure: " + t.message)
            }

            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.body() != null && response.body()!!.features().isNotEmpty()) {

                    selectedPlace = response.body()!!.features()[0]
                    if (selectedPlace != null) {
                        val address = selectedPlace!!.placeName()
                        if (address.isRequiredField()) {
                            ivDone.showView()
                            tvAddress.text = address.toString()
                        } else {
                            ivDone.hideView()
                        }
                    }
                }
            }

        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            if (mapBoxMap?.style != null) {
                enableLocationComponent(mapBoxMap?.style!!)
            }
        } else {
            showAlertDialog(
                R.string.missing_permission_message,
                R.string.missing_permission_title,
                R.string.permission,
                R.string.cancel, {
                    // this mean user has clicked on permission button to update run time permission.
                    openAppSetting()
                }
            )
        }
    }

    private val locationRequest = LocationRequest().apply {
        this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        this.numUpdates = 1
    }

    private val locationSettingRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)

    /**
     * this method will check required for location and according to result it will go ahead for fetching location.
     */
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingRequest.build())!!
            .addOnSuccessListener(this) {
                initLocationEngine()
            }.addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(TAG, resources.getString(R.string.location_settings_are_not_satisfied))
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(this, KeyUtils.REQUEST_CHECK_SETTINGS)
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i(TAG, getString(R.string.pendingintent_unable_to_execute_request))
//                            viewModel.fetchSavedLocation()
                            sie.printStackTrace()
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            resources.getString(R.string.location_settings_are_inadequate_and_cannot_be_fixed_here)
                        Logger.e(TAG, errorMessage)
//                        viewModel.fetchSavedLocation()
                    }
                }
            }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivBack -> onBackPressed()
            R.id.ivDone -> {
                selectedPlace ?: return
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(KeyUtils.SELECTED_PLACE, selectedPlace?.let { AddressMapperMapBoxMap.apply(it) })
                })
                finish()
            }
            R.id.tvAddress -> startVanillaMapBoxAutoCompleteActivity()
            R.id.iv_my_location -> enableLocationComponent(mapBoxMap?.style!!)
        }
    }

    private fun startVanillaMapBoxAutoCompleteActivity() {
        val intentPlacePicker = Intent(this, VanillaMapBoxAutoCompleteActivity::class.java)
        accessToken?.let {
            intentPlacePicker.putExtra(KeyUtils.MAPBOX_ACCESS_TOKEN, it)
        }
        minCharLimit.let {
            intentPlacePicker.putExtra(KeyUtils.MIN_CHAR_LIMIT, it)
        }
        limit?.let {
            intentPlacePicker.putExtra(KeyUtils.LIMIT, it)
        }
        language?.let {
            intentPlacePicker.putExtra(KeyUtils.LANGUAGE, it)
        }
        country?.let {
            intentPlacePicker.putExtra(KeyUtils.COUNTRY, it)
        }
        proximity?.let {
            intentPlacePicker.putExtra(KeyUtils.PROXIMITY, it)
        }
        types?.let {
            intentPlacePicker.putExtra(KeyUtils.TYPES, it)
        }
        startActivityForResult(intentPlacePicker, KeyUtils.REQUEST_PLACE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Check for the integer request code originally supplied to PlacePickerActivityForResult()
            KeyUtils.REQUEST_PLACE_PICKER -> when (resultCode) {
                Activity.RESULT_OK -> {
                    selectedPlace ?: return
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(KeyUtils.SELECTED_PLACE, data?.getSerializableExtra(KeyUtils.SELECTED_PLACE))
                    })
                    finish()
                }
            }
            // Check for the integer request code originally supplied to startResolutionForResult().
            KeyUtils.REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_CANCELED -> {
                }
                Activity.RESULT_OK -> {
                    enableLocationComponent(mapBoxMap?.style!!)
                }
            }
        }
    }

    private inner class LocationCallback : LocationEngineCallback<LocationEngineResult> {

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        override fun onSuccess(result: LocationEngineResult?) {
            val activity: VanillaMapBoxActivity = activityWeakReference.get()!!

            val location = result?.lastLocation ?: return

            // Pass the new location to the Maps SDK's LocationComponent
            if (activity.mapBoxMap != null && result.lastLocation != null) {
                activity.mapBoxMap!!.locationComponent.forceLocationUpdate(result.lastLocation)
            }

            location.let {
                viewModel.saveLatLngToSharedPref(location.latitude, location.longitude)
            }
            viewModel.fetchSavedLocation()
            locationEngine!!.removeLocationUpdates(this)
        }


        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        override fun onFailure(exception: Exception) {
        }

    }

    public override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Prevent leaks
        if (locationEngine != null) {
            locationEngine!!.removeLocationUpdates(callback)
        }
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}