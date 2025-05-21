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

import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.SystemLocationDataSource
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Wraps a [TransformationMatrixCameraController] and uses the position of the device to update the camera.
 * If the camera position deviates significantly from the device's position, the camera is updated.
 *
 * @see updateCamera to update the camera using the orientation of the [Frame.getCamera].
 * @see updateCamera to calibrate the camera using heading and elevation offsets.
 *
 * @param calibrationState a [CalibrationState] to use for elevation and heading calibration
 * @param clippingDistance the distance to the far clipping plane
 * @param onInitializationError called when an error occurs before the origin camera is initialized
 * @param onResetOriginCamera called when the origin camera is reset
 *
 * @since 200.7.0
 */
internal class WorldTrackingCameraController(
    private val calibrationState: CalibrationState,
    clippingDistance: Double?,
    private val onInitializationError: (Throwable) -> Unit,
    private val onResetOriginCamera: () -> Unit
) : WorldScaleCameraController {

    // This coroutine scope is tied to the lifecycle of this [LocationDataSourceWrapper]
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val worldScaleHeadingProvider: WorldScaleHeadingProvider
    private val locationDataSource =
        SystemLocationDataSource(userProvider = LocationManager.GPS_PROVIDER)

    override val cameraController = TransformationMatrixCameraController().apply {
        this.clippingDistance = clippingDistance
    }

    // keep track of the current location of the camera separately since Camera does not preserve
    // the vertical WKID of the location, which we need for calculating geodetic distances in shouldUpdateCamera()
    private var currentCameraLocation: Point? = null

    init {
        val applicationContext = ArcGISEnvironment.applicationContext
        require(applicationContext != null)

        worldScaleHeadingProvider = WorldScaleHeadingProvider(applicationContext, onInitializationError)
    }

    override var hasSetOriginCamera by mutableStateOf(false)
        private set

    /**
     * Sets the current position of the camera using the orientation of the [Frame.getCamera].
     *
     * @since 200.7.0
     */
    override fun updateCamera(frame: Frame, session: Session) {
        val cameraPosition = frame.camera.displayOrientedPose.transformationMatrix
        cameraController.transformationMatrix = cameraPosition
    }

    /**
     * Converts an ARCore [Pose] to a global [Point] using the Pose's offset from the origin camera.
     *
     * @since 200.8.0
     */
    override fun getPointFromPose(pose: Pose, session: Session): Point {
        val hitPoseTransformationMatrix = pose.transformationMatrix
        val origin = cameraController.originCamera.value.transformationMatrix
        return Camera(origin + hitPoseTransformationMatrix).location
    }

    /**
     * Sets the origin position of the camera to the given location and heading.
     *
     * @since 200.7.0
     */
    private fun updateCamera(location: Location, heading: Float) {
        GeometryEngine.projectOrNull(location.position, WorldScaleParameters.SR_CAMERA)
            ?.let { projectedLocation ->
                // cache the location of the origin camera for later use
                currentCameraLocation = projectedLocation

                cameraController.setOriginCamera(
                    Camera(
                        projectedLocation.y,
                        projectedLocation.x,
                        if (projectedLocation.hasZ) projectedLocation.z
                            ?: calibrationState.totalElevationOffset else calibrationState.totalElevationOffset,
                        heading + calibrationState.totalHeadingOffset,
                        90.0,
                        0.0
                    )
                )
            }
    }

    /**
     * Rotates the origin position of the camera by the given heading offset.
     *
     * @since 200.7.0
     */
    private fun updateCameraHeading(headingOffset: Double) {
        cameraController.setOriginCamera(
            cameraController.originCamera.value
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
        cameraController.setOriginCamera(
            cameraController.originCamera.value
                .elevate(elevationOffset)
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            worldScaleHeadingProvider.stop()
            scope.cancel()
        }
        super.onDestroy(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            worldScaleHeadingProvider.stop()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        scope.launch {
            worldScaleHeadingProvider.start()
            locationDataSource.start()
        }
        scope.launch {
            locationDataSource.status.filterIsInstance<LocationDataSourceStatus.FailedToStart>()
                .collect {
                    locationDataSource.error.value?.let { error ->
                        onInitializationError(
                            error
                        )
                    }
                }
        }
        scope.launch {
            locationDataSource.locationChanged
                .filter { location ->
                    shouldUpdateCamera(
                        location,
                        currentCameraLocation
                    )
                }
                .collect { location ->
                    updateCamera(location, worldScaleHeadingProvider.headings.first())
                    // We have to do this or the error gets bigger and bigger.
                    cameraController.transformationMatrix =
                        TransformationMatrix.createIdentityMatrix()
                    onResetOriginCamera()
                    hasSetOriginCamera = true
                }
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
 * Evaluates a location to determine if the camera should be updated.
 *
 * Returns false if the location timestamp is older than a threshold,
 * if the horizontal or vertical accuracy is negative,
 * or if the distance between the location and the current camera is less than a threshold.
 * Otherwise, returns true.
 *
 * @since 200.7.0
 */
internal fun shouldUpdateCamera(
    location: Location,
    currentCameraLocation: Point?
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

    // filter out locations with a NaN z value
    if (location.position.z?.isNaN() == true) return false

    // filter out locations with low accuracy
    if (location.horizontalAccuracy > WorldScaleParameters.HORIZONTAL_ACCURACY_THRESHOLD_METERS) return false

    // if we don't have a location of the current camera, don't measure the distance
    if (currentCameraLocation == null) return true

    val projectedLocation =
        GeometryEngine.projectOrNull(location.position, WorldScaleParameters.SR_CAMERA)
            ?: return false

    val distance = GeometryEngine.distanceGeodeticOrNull(
        currentCameraLocation,
        projectedLocation,
        distanceUnit = LinearUnit.meters,
        azimuthUnit = null,
        curveType = GeodeticCurveType.Geodesic
    )?.distance ?: return false

    return distance > WorldScaleParameters.LOCATION_DISTANCE_THRESHOLD_METERS
}
