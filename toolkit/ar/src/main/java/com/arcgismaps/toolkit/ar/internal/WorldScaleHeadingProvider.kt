package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.util.Log
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class WorldScaleHeadingProvider(context: Context) {

    private val fusedOrientationProviderClient = LocationServices.getFusedOrientationProviderClient(context)
    private val listener = DeviceOrientationListener { deviceOrientation ->
        _headings.tryEmit(deviceOrientation.headingDegrees)
    }

    private val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _headings = MutableSharedFlow<Float>()
    val headings = _headings.asSharedFlow()

    fun start(){
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
            .addOnSuccessListener { Log.d("WSHP", "Registration successful") }
            .addOnFailureListener { Log.e("WSHP", it.message.toString()) }
    }

    fun stop(){
        fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }
}
