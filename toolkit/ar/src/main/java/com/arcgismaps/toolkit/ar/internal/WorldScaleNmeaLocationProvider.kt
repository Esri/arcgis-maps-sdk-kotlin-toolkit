/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Provides locations and headings to a [CustomLocationDataSource] using NMEA messages from the device's GPS.
 *
 * NMEA messages are used because this enables us to read the orthometric height (height above geoid) from the GPS,
 * whereas normal Android location updates only provide the ellipsoidal height.
 *
 * @throws IllegalStateException if [ArcGISEnvironment.applicationContext] is null.
 * @since 200.7.0
 */
internal class WorldScaleNmeaLocationProvider(scope: CoroutineScope) :
    CustomLocationDataSource.LocationProvider, OnNmeaMessageListener {

    private val systemLocationDataSource = SystemLocationDataSource()
    private val nmeaLocationDataSource = NmeaLocationDataSource()
    private val locationManager: LocationManager
    private val handler: Handler

    init {
        val applicationContext = ArcGISEnvironment.applicationContext
        require(applicationContext != null)
        locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handler = Handler(applicationContext.mainLooper)
    }

    override val headings: Flow<Double> = emptyFlow()

    override val locations: Flow<Location> =
        nmeaLocationDataSource.locationChanged.map {
            Location.create(
                Point(
                    it.position.x,
                    it.position.y,
                    it.heightAboveGeoid,
                    it.position.m,
                    SpatialReference(it.position.spatialReference?.wkid ?: WKID_WGS84, WKID_EGM96)
                ),
                it.horizontalAccuracy,
                it.verticalAccuracy,
                it.speed,
                it.course,
                it.lastKnown,
                it.timestamp,
                it.additionalSourceProperties
            )
        }.filterNotNull()

    /**
     * Starts listening for NMEA messages from the GPS and headings from a [SystemLocationDataSource].
     *
     * @throws IllegalStateException if [ArcGISEnvironment.applicationContext] is null.
     * @since 200.7.0
     */
    @SuppressLint("MissingPermission")
    internal suspend fun start() {
        val applicationContext = ArcGISEnvironment.applicationContext
        require(applicationContext != null)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100,
                0f,
                {
                },
                applicationContext.mainLooper
            )
        }
        systemLocationDataSource.start()
        nmeaLocationDataSource.start()
        locationManager.addNmeaListener(this@WorldScaleNmeaLocationProvider, handler)
    }

    /**
     * Stops listening for NMEA messages from the GPS and headings from a [SystemLocationDataSource].
     *
     * @since 200.7.0
     */
    internal suspend fun stop() {
        locationManager.removeNmeaListener(this)
        systemLocationDataSource.stop()
        nmeaLocationDataSource.stop()
    }

    override fun onNmeaMessage(message: String?, timestamp: Long) {
        nmeaLocationDataSource.pushData(message?.toByteArray())
    }

    companion object {
        private const val WKID_WGS84 = 4326
        private const val WKID_EGM96 = 5773
    }
}
