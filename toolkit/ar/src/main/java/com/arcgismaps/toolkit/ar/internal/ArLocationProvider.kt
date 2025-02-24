package com.arcgismaps.toolkit.ar.internal

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
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

/**
 * Provides locations and headings to a [CustomLocationDataSource] using NMEA messages from the device's GPS.
 *
 * NMEA messages are used because this enables us to read the orthometric height (height above geoid) from the GPS,
 * whereas normal Android location updates only provide the ellipsoidal height.
 *
 * @since 200.7.0
 */
internal class ArLocationProvider(private val scope: CoroutineScope) :
    CustomLocationDataSource.LocationProvider, OnNmeaMessageListener {

    private val systemLocationDataSource = SystemLocationDataSource()
    private val nmeaLocationDataSource = NmeaLocationDataSource()
    private val locationManager: LocationManager
    private val handler: Handler

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
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100,
                0f,
                {
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
