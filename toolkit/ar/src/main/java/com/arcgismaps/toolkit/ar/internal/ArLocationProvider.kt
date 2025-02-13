package com.arcgismaps.toolkit.ar.internal

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
import android.os.Looper
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.Location
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.location.SystemLocationDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

internal class ArLocationProvider(scope: CoroutineScope) : CustomLocationDataSource.LocationProvider, OnNmeaMessageListener {

    private val systemLocationDataSource = SystemLocationDataSource()
    private val nmeaLocationDataSource = NmeaLocationDataSource()
    private val locationManager: LocationManager
    private val handler: Handler

    init {
        require(ArcGISEnvironment.applicationContext != null)
        locationManager = ArcGISEnvironment.applicationContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handler = Handler(ArcGISEnvironment.applicationContext!!.mainLooper)
    }

    override val headings: SharedFlow<Double> = systemLocationDataSource.headingChanged.shareIn(
        scope = scope,
        replay = 1,
        started = SharingStarted.WhileSubscribed()
    )
    override val locations: Flow<Location> = nmeaLocationDataSource.locationChanged.map {
        Location.create(
            Point(
                it.position.x,
                it.position.y,
                it.heightAboveGeoid,
                it.position.m,
                it.position.spatialReference
            ),
            it.horizontalAccuracy,
            it.verticalAccuracy,
            it.speed,
            headings.replayCache[0], // just use the last value emitted by the systemLocationDataSource's headingChanged flow
            it.lastKnown,
            it.timestamp,
            it.additionalSourceProperties
        )
    }.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed()
    )

    @SuppressLint("MissingPermission")
    internal suspend fun start() = withContext(Dispatchers.IO){
        systemLocationDataSource.start()
        nmeaLocationDataSource.start()

        val selectedLocationProviders = mutableListOf<String>()
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) selectedLocationProviders.add(
            LocationManager.NETWORK_PROVIDER
        )
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) selectedLocationProviders.add(
            LocationManager.GPS_PROVIDER
        )
        locationManager.addNmeaListener(this@ArLocationProvider, handler)
        selectedLocationProviders.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                100,
                0f,
                {},
                ArcGISEnvironment.applicationContext!!.mainLooper
            )
        }
    }

    internal suspend fun stop() {
        systemLocationDataSource.stop()
        nmeaLocationDataSource.stop()
        locationManager.removeNmeaListener(this)
    }

    override fun onNmeaMessage(message: String?, timestamp: Long) {
        nmeaLocationDataSource.pushData(message?.toByteArray())
    }
}