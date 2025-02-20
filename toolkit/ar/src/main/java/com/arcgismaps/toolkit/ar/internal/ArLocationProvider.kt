package com.arcgismaps.toolkit.ar.internal

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.Location
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.location.SystemLocationDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

internal class ArLocationProvider(private val scope: CoroutineScope) :
    CustomLocationDataSource.LocationProvider, OnNmeaMessageListener {

    private val systemLocationDataSource = SystemLocationDataSource()
    private val nmeaLocationDataSource = NmeaLocationDataSource()
    private val locationManager: LocationManager
    private val handler: Handler

    private var lastKnownBearing: Float? = null

    internal val debugInfo = mutableStateOf(
        ArLocationProviderDebugInfo(
            hasBearing = false,
            bearingAccuracyDegrees = 0f,
            bearing = 0f,
            satelliteCount = 0
        )
    )


    init {
        require(ArcGISEnvironment.applicationContext != null)
        locationManager =
            ArcGISEnvironment.applicationContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handler = Handler(ArcGISEnvironment.applicationContext!!.mainLooper)
    }

    override val headings: SharedFlow<Double> = systemLocationDataSource.headingChanged.shareIn(
        scope = scope,
        replay = 1,
        started = SharingStarted.WhileSubscribed()
    )
    override val locations: Flow<Location> =
        nmeaLocationDataSource.locationChanged.map {
            Location.create(
                Point(
                    it.position.x,
                    it.position.y,
                    it.heightAboveGeoid,
                    it.position.m,
                    SpatialReference(it.position.spatialReference!!.wkid, 5773 /*EGM96*/)
                ),
                it.horizontalAccuracy,
                it.verticalAccuracy,
                it.speed,
                headings.replayCache[0],
                it.lastKnown,
                it.timestamp,
                it.additionalSourceProperties
            )
        }.filterNotNull()

    @SuppressLint("MissingPermission")
    internal suspend fun start() {
        val selectedLocationProviders = mutableListOf<String>()
//        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) selectedLocationProviders.add(
//            LocationManager.NETWORK_PROVIDER
//        )
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) selectedLocationProviders.add(
            LocationManager.GPS_PROVIDER
        )
        selectedLocationProviders.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                100,
                0f,
                {
                    if (it.hasBearing() && it.bearingAccuracyDegrees < 60.0) {
                        lastKnownBearing = it.bearing
                    }
                    Log.e("ArLocationProvider", "Satellites: ${it.extras?.getInt("satellites")}")
                    Log.e("ArLocationProvider", "Has bearing: ${it.hasBearing()}")
                    Log.e("ArLocationProvider", "Bearing accuracy: ${it.bearingAccuracyDegrees}")
                    Log.e("ArLocationProvider", "Bearing: ${it.bearing}")
                    debugInfo.value =
                        ArLocationProviderDebugInfo(
                            hasBearing = it.hasBearing(),
                            bearingAccuracyDegrees = it.bearingAccuracyDegrees,
                            bearing = it.bearing,
                            satelliteCount = it.extras?.getInt("satellites") ?: 0
                        )
                },
                ArcGISEnvironment.applicationContext!!.mainLooper
            )
        }
        systemLocationDataSource.start()
        nmeaLocationDataSource.start()
        locationManager.addNmeaListener(this@ArLocationProvider, handler)
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

internal data class ArLocationProviderDebugInfo(
    val hasBearing: Boolean,
    val bearingAccuracyDegrees: Float,
    val bearing: Float,
    val satelliteCount: Int
)