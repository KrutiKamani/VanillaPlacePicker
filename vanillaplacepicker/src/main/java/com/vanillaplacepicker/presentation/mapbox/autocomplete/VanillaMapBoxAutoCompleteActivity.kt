package com.vanillaplacepicker.presentation.mapbox.autocomplete

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding2.widget.RxTextView
import com.vanillaplacepicker.R
import com.vanillaplacepicker.data.MapBoxGeoCoderAddressResponse
import com.vanillaplacepicker.data.common.AddressMapperMapBoxGeoCoder
import com.vanillaplacepicker.domain.common.Resource
import com.vanillaplacepicker.domain.common.SafeObserver
import com.vanillaplacepicker.domain.common.Status
import com.vanillaplacepicker.extenstion.hasExtra
import com.vanillaplacepicker.extenstion.hideView
import com.vanillaplacepicker.extenstion.showView
import com.vanillaplacepicker.presentation.common.VanillaBaseViewModelActivity
import com.vanillaplacepicker.utils.KeyUtils
import kotlinx.android.synthetic.main.activity_mi_placepicker.*
import kotlinx.android.synthetic.main.lo_recyclremptyvw_appearhere.*

class VanillaMapBoxAutoCompleteActivity : VanillaBaseViewModelActivity<VanillaMapBoxAutoCompleteViewModel>(),
    View.OnClickListener {

    private val TAG = VanillaMapBoxAutoCompleteActivity::class.java.simpleName

    private var accessToken: String? = null
    private var minCharLimit: Int = 3
    private var limit: Int? = null
    private var language: String? = null
    private var proximity: String? = null
    private var types: String? = null

    private val autoCompleteAdapter by lazy { VanillaMapBoxAutoCompleteAdapter(this::onItemSelected) }

    override fun buildViewModel(): VanillaMapBoxAutoCompleteViewModel {
        return ViewModelProviders.of(this)[VanillaMapBoxAutoCompleteViewModel::class.java]
    }

    override fun getContentResource() = R.layout.activity_mi_placepicker

    override fun initViews() {
        super.initViews()
        // HIDE ActionBar(if exist in style) of root project module
        supportActionBar?.hide()
        getBundle()
        ivBack.setOnClickListener(this)
        ivClear.setOnClickListener(this)
        rvPlaces?.setHasFixedSize(true)
        rvPlaces?.setEmptyView(rvEmptyView)
        rvPlaces?.adapter = autoCompleteAdapter

        // call this method only once
        val hashMap = HashMap<String, String>()
        hashMap[KeyUtils.MAPBOX_ACCESS_TOKEN] = accessToken!!
        limit?.let {
            hashMap[KeyUtils.LIMIT] = limit.toString()
        }
        language?.let {
            hashMap.put(KeyUtils.LANGUAGE, it)
        }
        proximity?.let {
            hashMap.put(KeyUtils.PROXIMITY, it)
        }
        types?.let {
            hashMap.put(KeyUtils.TYPES, it)
        }

        viewModel.configureAutoComplete(minCharLimit, hashMap)

        RxTextView.afterTextChangeEvents(etQuery)
            .skipInitialValue()
            .subscribe {
                viewModel.onInputStateChanged(it.editable()?.trim().toString(), minCharLimit)
            }.collect()
    }

    private fun getBundle() {
        if (hasExtra(KeyUtils.MAPBOX_ACCESS_TOKEN)) {
            accessToken = intent.getStringExtra(KeyUtils.MAPBOX_ACCESS_TOKEN)
        }

        if (hasExtra(KeyUtils.LANGUAGE)) {
            language = intent.getStringExtra(KeyUtils.LANGUAGE)
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
        viewModel.showClearButtonLiveData.observe(this, SafeObserver(this::handleClearButtonVisibility))
        viewModel.autoCompleteLiveData.observe(this, SafeObserver(this::handleAutoCompleteData))
    }

    private fun handleClearButtonVisibility(visible: Boolean) {
        if (visible) ivClear.showView()
        else ivClear.hideView()
    }

    private fun handleAutoCompleteData(response: Resource<MapBoxGeoCoderAddressResponse>) {
        when (response.status) {
            Status.LOADING -> progressBar.showView()
            Status.SUCCESS -> handleSuccessResponse(response.item)
            Status.ERROR -> handleErrorResponse(response)
        }
    }

    private fun handleSuccessResponse(result: MapBoxGeoCoderAddressResponse?) {
        progressBar.hideView()
        result?.let {
            autoCompleteAdapter.setList(it.features!!)
        }
    }

    private fun handleErrorResponse(response: Resource<MapBoxGeoCoderAddressResponse>) {
        progressBar.hideView()
        autoCompleteAdapter.clearList()
        response.throwable?.let {
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivBack -> onBackPressed()
            R.id.ivClear -> {
                etQuery.text = null
            }
        }
    }

    private fun onItemSelected(selectedPlace: MapBoxGeoCoderAddressResponse.Features) {
        val intent = Intent().apply {
            putExtra(KeyUtils.SELECTED_PLACE, AddressMapperMapBoxGeoCoder.apply(selectedPlace))
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}