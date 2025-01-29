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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDataSource
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.SystemLocationDataSource
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Wrapper class to manage the lifecycle of a [LocationDataSource].
 * The [LocationDataSource] will be started when the lifecycle owner is resumed and stopped when the
 * lifecycle owner is paused or destroyed.
 *
 * @since 200.7.0
 */
internal class WorldTrackingCameraController(private val onLocationDataSourceFailedToStart: (Throwable) -> Unit) :
    DefaultLifecycleObserver {

    // This coroutine scope is tied to the lifecycle of this [LocationDataSourceWrapper]
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val locationDataSource = SystemLocationDataSource()
    val cameraController = TransformationMatrixCameraController()
    // TODO: make this a mutable state
    internal var hasSetOriginCamera by mutableStateOf(false)
        private set

    internal fun updateCamera(frame: Frame): Unit {
        val cameraPosition = frame.camera.displayOrientedPose.transformationMatrix
        cameraController.transformationMatrix = cameraPosition
    }

    internal fun updateCamera(headingOffset: Double, elevationOffset: Double): Unit = TODO()

    internal fun updateCamera(location: Location): Unit = TODO()

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            scope.cancel()
        }
        super.onDestroy(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        scope.launch {
            locationDataSource.start()
        }
        scope.launch {
            locationDataSource.status.filterIsInstance<LocationDataSourceStatus.FailedToStart>()
                .collect {
                    locationDataSource.error.value?.let { error ->
                        onLocationDataSourceFailedToStart(
                            error
                        )
                    }
                }
        }
        scope.launch {
            locationDataSource.locationChanged
                .filter { location ->
                    !hasSetOriginCamera || shouldUpdateCamera(
                        location,
                        cameraController.originCamera.value
                    )
                }
                .collect { location ->
                    cameraController.setOriginCamera(
                        Camera(
                            location.position.y,
                            location.position.x,
                            if (location.position.hasZ) location.position.z!! else 0.0,
                            0.0,
                            90.0,
                            0.0
                        )
                    )
                    // We have to do this or the error gets bigger and bigger.
                    cameraController.transformationMatrix =
                        TransformationMatrix.createIdentityMatrix()
                    if (!hasSetOriginCamera) {
                        hasSetOriginCamera = true
                    }
                }
        }
    }
}


@Composable
internal fun rememberWorldTrackingCameraController(onLocationDataSourceFailedToStart: (Throwable) -> Unit): WorldTrackingCameraController {
    ArcGISEnvironment.applicationContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val wrapper = remember { WorldTrackingCameraController(onLocationDataSourceFailedToStart) }
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(wrapper)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(wrapper)
            wrapper.onDestroy(lifecycleOwner)
        }
    }
    return wrapper
}

/**
 * Returns false if the location timestamp is older than 10 seconds,
 * if the horizontal or vertical accuracy is negative,
 * or if the distance between the location and the current camera is less than 2 meters.
 * Otherwise, returns true.
 *
 * @since 200.7.0
 */
internal fun shouldUpdateCamera(
    location: Location,
    currentCamera: Camera,
): Boolean {
    // filter out old locations
    if (Instant.now()
            .toEpochMilli() - location.timestamp.toEpochMilli() > WorldScaleParameters.LOCATION_AGE_THRESHOLD_MS
    ) return false

    // filter out locations with no accuracy
    if (location.horizontalAccuracy < 0.0
        || location.verticalAccuracy < 0.0
        || location.horizontalAccuracy.isNaN()
        || location.verticalAccuracy.isNaN()
    ) return false

    val distance = GeometryEngine.distanceGeodeticOrNull(
        currentCamera.location,
        location.position,
        distanceUnit = LinearUnit.meters,
        azimuthUnit = null,
        curveType = GeodeticCurveType.Geodesic
    )?.distance ?: return false
    return distance > WorldScaleParameters.LOCATION_DISTANCE_THRESHOLD_METERS
}
