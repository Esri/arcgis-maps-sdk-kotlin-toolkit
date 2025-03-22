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

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.launch

/**
 * Wraps a [TransformationMatrixCameraController] and uses Google's Geospatial API to update the camera's position.
 *
 * This class should not be constructed directly. Instead, use the [rememberGeospatialTrackingCameraController] factory function.
 *
 * @see updateCamera to update the camera using the orientation of the [Frame.getCamera].
 * @see rememberGeospatialTrackingCameraController
 *
 * @since 200.7.0
 */
internal class GeospatialTrackingCameraController(
    private val calibrationState: CalibrationState,
    context: Context,
) : WorldScaleCameraController {
    override val cameraController = TransformationMatrixCameraController()
    override var hasSetOriginCamera: Boolean by mutableStateOf(false)
        private set

    private val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
    }

    override fun updateCamera(frame: Frame, session: Session) {
        session.earth?.let { earth ->
            if (earth.trackingState != TrackingState.TRACKING) return@let
            if (earth.earthState != Earth.EarthState.ENABLED) return@let

            val geospatialPose = earth.cameraGeospatialPose

            // project the geospatial pose to the camera's coordinate system
            val projectedLocation = GeometryEngine.projectOrNull(
                Point(
                    geospatialPose.longitude,
                    geospatialPose.latitude,
                    geospatialPose.altitude + calibrationState.totalElevationOffset,
                    WorldScaleParameters.SR_WGS84_WGS_VERTICAL
                ),
                WorldScaleParameters.SR_CAMERA
            ) ?: return@let

            // set the origin camera for lat, lon and altitude (projectedLocation)
            cameraController.setOriginCamera(
                Camera(
                    projectedLocation,
                    calibrationState.totalHeadingOffset,
                    90.0,
                    0.0
                )
            )

            // set the transformation matrix for orienting the scene camera
            val displayOrientedPose = frame.camera.displayOrientedPose
            cameraController.transformationMatrix =
                TransformationMatrix.createWithQuaternionAndTranslation(
                    displayOrientedPose.qx().toDouble(),
                    displayOrientedPose.qy().toDouble(),
                    displayOrientedPose.qz().toDouble(),
                    displayOrientedPose.qw().toDouble(),
                    0.0,
                    0.0,
                    0.0
                )

            hasSetOriginCamera = true
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

    override fun onResume(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            calibrationState.headingDeltas.collect {
                updateCameraHeading(-it)
            }
        }
        owner.lifecycleScope.launch {
            calibrationState.elevationDeltas.collect {
                updateCameraElevation(it)
            }
        }
        super.onResume(owner)
    }
}

/**
 * Returns a [GeospatialTrackingCameraController].
 *
 * @see GeospatialTrackingCameraController
 * @since 200.7.0
 */
@Composable
internal fun rememberGeospatialTrackingCameraController(
    calibrationState: CalibrationState,
): WorldScaleCameraController {
    val context = LocalContext.current
    return remember {
        GeospatialTrackingCameraController(
            calibrationState,
            context
        )
    }
}
