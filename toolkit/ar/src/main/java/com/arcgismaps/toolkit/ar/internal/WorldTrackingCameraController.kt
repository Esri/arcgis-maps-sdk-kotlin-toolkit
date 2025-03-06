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
import com.arcgismaps.geometry.Point
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDataSource
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Wraps a [TransformationMatrixCameraController] and uses the position of the device to update the camera.
 * If the camera position deviates significantly from the device's position, the camera is updated.
 *
 * This class should not be constructed directly. Instead, use the [rememberWorldTrackingCameraController] factory function.
 *
 * @see updateCamera to update the camera using the orientation of the [Frame.getCamera].
 * @see updateCamera to calibrate the camera using heading and elevation offsets.
 * @see rememberWorldTrackingCameraController
 *
 * @since 200.7.0
 */
internal class WorldTrackingCameraController(
    private val calibrationState: CalibrationState,
    private val scene: ArcGISScene,
    private val onLocationDataSourceFailedToStart: (Throwable) -> Unit
) :
    DefaultLifecycleObserver {

    // This coroutine scope is tied to the lifecycle of this [LocationDataSourceWrapper]
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val worldScaleNmeaLocationProvider = WorldScaleNmeaLocationProvider(scope)
    private val locationDataSource = CustomLocationDataSource {
        worldScaleNmeaLocationProvider
    }
    val cameraController = TransformationMatrixCameraController()

    internal var hasSetOriginCamera by mutableStateOf(false)
        private set

    /**
     * Sets the current position of the camera using the orientation of the [Frame.getCamera].
     *
     * @since 200.7.0
     */
    internal fun updateCamera(frame: Frame) {
        val cameraPosition = frame.camera.displayOrientedPose.transformationMatrix
        cameraController.transformationMatrix = cameraPosition
    }

    /**
     * Sets the origin position of the camera to the given location.
     *
     * @since 200.7.0
     */
    private fun updateCamera(location: Location) {
        // We have to do this or the error gets bigger and bigger.
        cameraController.transformationMatrix =
            TransformationMatrix.createIdentityMatrix()
        if (!hasSetOriginCamera) {
            hasSetOriginCamera = true
        }
        cameraController.setOriginCamera(
            Camera(
                location.position.y,
                location.position.x,
                if (location.position.hasZ) location.position.z?.plus(calibrationState.totalElevationOffset)
                    ?: calibrationState.totalElevationOffset else calibrationState.totalElevationOffset,
                calibrationState.totalHeadingOffset,
                90.0,
                0.0
            )
        )
    }

    private fun updateCamera(elevationInfo: WorldScaleSurfaceElevationProvider.ElevationInfo) {
        // We have to do this or the error gets bigger and bigger.
        cameraController.transformationMatrix =
            TransformationMatrix.createIdentityMatrix()
        if (!hasSetOriginCamera) {
            hasSetOriginCamera = true
        }
        cameraController.setOriginCamera(
            Camera(
                elevationInfo.location.y,
                elevationInfo.location.x,
                elevationInfo.elevation + calibrationState.totalElevationOffset,
                calibrationState.totalHeadingOffset,
                90.0,
                0.0
            )
        )
    }

    /**
     * Rotates the origin position of the camera by the given heading offset.
     *
     * @since 200.7.0
     */
    private fun updateCameraHeading(headingOffset: Double) {
        cameraController.setOriginCamera(cameraController.originCamera.value
            .rotateAround(
                targetPoint = cameraController.originCamera.value.location,
                deltaHeading = headingOffset,
                deltaPitch = 0.0,
                deltaRoll = 0.0
            )
        )
    }

    /**
     * Elevates the origin position of the camera by the given elevation offset.
     *
     * @since 200.7.0
     */
    private fun updateCameraElevation(elevationOffset: Double) {
        cameraController.setOriginCamera(cameraController.originCamera.value
            .elevate(elevationOffset)
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            worldScaleNmeaLocationProvider.stop()
            scope.cancel()
        }
        super.onDestroy(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            worldScaleNmeaLocationProvider.stop()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        scope.launch {
            worldScaleNmeaLocationProvider.start()
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
            val location = locationDataSource.locationChanged
                .filter { location ->
                    shouldUpdateCamera(
                        location
                    )
                }
                .map { location ->
                    val elevation = scene.load().mapCatching {
                        val elevation = scene.baseSurface.getElevation(location.position)
                        elevation.getOrThrow()
                    }.getOrElse {
                        if (location.position.hasZ) location.position.z ?: 0.0 else 0.0
                    }
                    Location.create(
                        position = Point(
                            location.position.x,
                            location.position.y,
                            elevation,
                            location.position.spatialReference
                        ),
                        horizontalAccuracy = location.horizontalAccuracy,
                        verticalAccuracy = location.verticalAccuracy,
                        speed = location.speed,
                        course = location.course,
                        lastKnown = location.lastKnown,
                        timestamp = location.timestamp,
                        additionalSourceProperties = location.additionalSourceProperties
                    )
                }
                .take(5)
                .toList()
                .minBy { it.horizontalAccuracy }

            updateCamera(location)
        }
        scope.launch {
            calibrationState.headingDeltas.collect {
                updateCameraHeading(-it)
            }
        }
        scope.launch {
            calibrationState.elevationDeltas.collect {
                updateCameraElevation(it)
            }
        }
    }
}

/**
 * Returns a [WorldTrackingCameraController] that is tied to the lifecycle of the current [LifecycleOwner].
 *
 * @see WorldTrackingCameraController
 * @param onLocationDataSourceFailedToStart Callback that is called when the [LocationDataSource] fails to start.
 * @since 200.7.0
 */
@Composable
internal fun rememberWorldTrackingCameraController(
    calibrationState: CalibrationState,
    scene: ArcGISScene,
    onLocationDataSourceFailedToStart: (Throwable) -> Unit): WorldTrackingCameraController {
    ArcGISEnvironment.applicationContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val wrapper = remember {
        WorldTrackingCameraController(
            calibrationState,
            scene,
            onLocationDataSourceFailedToStart
        )
    }
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
    location: Location
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


    // filter out locations with low accuracy
    if (location.horizontalAccuracy > 6.0) return false
    if (location.verticalAccuracy > 6.0) return false

    return true
}
