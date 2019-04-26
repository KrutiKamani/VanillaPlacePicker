package com.vanillaplacepicker.presentation.mapbox.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
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
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.vanillaplacepicker.R
import com.vanillaplacepicker.data.common.AddressMapperMapBoxMap
import com.vanillaplacepicker.domain.common.SafeObserver
import com.vanillaplacepicker.extenstion.hasExtra
import com.vanillaplacepicker.extenstion.hideView
import com.vanillaplacepicker.extenstion.isRequiredField
import com.vanillaplacepicker.extenstion.showView
import com.vanillaplacepicker.presentation.common.VanillaBaseViewModelActivity
import com.vanillaplacepicker.presentation.mapbox.autocomplete.VanillaMapBoxAutoCompleteActivity
import com.vanillaplacepicker.utils.KeyUtils
import com.vanillaplacepicker.utils.SharedPrefs
import kotlinx.android.synthetic.main.activity_mapbox_map.*
import kotlinx.android.synthetic.main.toolbar.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VanillaMapBoxActivity : VanillaBaseViewModelActivity<VanillaMapBoxViewModel>(), OnMapReadyCallback,
    View.OnClickListener, PermissionsListener, LocationEngineCallback<LocationEngineResult> {

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
    private var language: String? = null

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private val sharedPrefs by lazy { SharedPrefs(this) }

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

//        mapView?.onCreate(savedInstanceState)
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

        if (hasExtra(KeyUtils.MIN_CHAR_LIMIT)) {
            minCharLimit = intent.getIntExtra(KeyUtils.MIN_CHAR_LIMIT, 3)
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
                // Check if permissions are enabled and if not request
                if (PermissionsManager.areLocationPermissionsGranted(this)) {
                    getLocation()
                } else {
                    requestForLocationPermission()
                }
            } else {
                moveCameraToPosition(LatLng(latitude, longitude))
            }
        }
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

        mapBoxMap?.setPadding(0, 256, 0, 0)
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
                if (response.body() != null) {

                    selectedPlace = response.body()!!.features()[0]
                    if (selectedPlace != null) {
                        // Get the first Feature from the successful geocoding response
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

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        locationEngine!!.getLastLocation(this)
    }

    override fun onSuccess(result: LocationEngineResult?) {
        val location = result?.lastLocation

        location?.let {
            viewModel.saveLatLngToSharedPref(location.latitude, location.longitude)
        }
        viewModel.fetchSavedLocation()
        locationEngine!!.removeLocationUpdates(this)
    }

    override fun onFailure(exception: java.lang.Exception) {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            getLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
            finish()
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
        language.let {
            intentPlacePicker.putExtra(KeyUtils.LANGUAGE, it)
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
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}