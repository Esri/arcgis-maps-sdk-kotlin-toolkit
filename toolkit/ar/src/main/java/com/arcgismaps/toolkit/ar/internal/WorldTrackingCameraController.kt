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
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.CustomLocationDataSource
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
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
    private val onLocationDataSourceFailedToStart: (Throwable) -> Unit,
    private val onResetSession: () -> Unit
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

    private var totalHeadingOffset = 0.0
    private var totalElevationOffset = 0.0

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
     * Modifies the origin position of the camera by the given heading and elevation offsets.
     *
     * @since 200.7.0
     */
    internal fun updateCamera(headingOffset: Double, elevationOffset: Double) {
        cameraController.setOriginCamera(cameraController.originCamera.value
            .rotateAround(
                targetPoint = cameraController.originCamera.value.location,
                deltaHeading = headingOffset,
                deltaPitch = 0.0,
                deltaRoll = 0.0
            ).elevate(elevationOffset)
        )

        totalHeadingOffset += headingOffset
        totalElevationOffset += elevationOffset
    }

    /**
     * Sets the origin position of the camera to the given location.
     *
     * @since 200.7.0
     */
    private fun updateCamera(location: Location) =
        cameraController.setOriginCamera(
            Camera(
                location.position.y,
                location.position.x,
                if (location.position.hasZ) location.position.z ?: totalElevationOffset else totalElevationOffset,
                totalHeadingOffset,
                90.0,
                0.0
            )
        )

    internal fun resetHeadingOffset(){
        cameraController.setOriginCamera(cameraController.originCamera.value
            .rotateAround(
                targetPoint = cameraController.originCamera.value.location,
                deltaHeading = -totalHeadingOffset,
                deltaPitch = 0.0,
                deltaRoll = 0.0
            )
        )
        totalHeadingOffset = 0.0
    }

    internal fun resetElevationOffset(){
        cameraController.setOriginCamera(cameraController.originCamera.value
            .elevate(-totalElevationOffset))
        totalElevationOffset = 0.0
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
                        location,
                        cameraController.originCamera.value
                    ) || !hasSetOriginCamera
                }.collect { location ->
                    updateCamera(location)
                    // We have to do this or the error gets bigger and bigger.
                    cameraController.transformationMatrix =
                        TransformationMatrix.createIdentityMatrix()
                    onResetSession()
                    if (!hasSetOriginCamera) {
                        hasSetOriginCamera = true
                    }
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
    onLocationDataSourceFailedToStart: (Throwable) -> Unit,
    onResetSession: () -> Unit
): WorldTrackingCameraController {
    ArcGISEnvironment.applicationContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val wrapper = remember { WorldTrackingCameraController(onLocationDataSourceFailedToStart, onResetSession) }
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
    currentOriginCamera: Camera,
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

    val currentOriginCameraPosition = Point(currentOriginCamera.location.x, currentOriginCamera.location.y, currentOriginCamera.location.z!!, location.position.spatialReference) ?: return false //, SpatialReference(currentOriginCamera.location.spatialReference?.wkid ?: 4326, 5773 /*EGM96*/))//GeometryEngine.projectOrNull(currentCamera.location, SpatialReference(4326, 115700)) ?: return false
    val distance = GeometryEngine.distanceGeodeticOrNull (
        currentOriginCameraPosition,
        location.position,
        distanceUnit = LinearUnit.meters,
        azimuthUnit = null,
        curveType = GeodeticCurveType.Geodesic
    )?.distance ?: return false

    return distance > WorldScaleParameters.LOCATION_DISTANCE_THRESHOLD_METERS
}
