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
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.sqrt

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
//        sessionWrapper.withLock { session, _ ->
            session.earth?.let { earth ->
                if (earth.trackingState != TrackingState.TRACKING) return@let
                if (earth.earthState != Earth.EarthState.ENABLED) return@let
                // TODO: early exit if session is paused?

                try {
                    val geospatialPose = earth.cameraGeospatialPose
                    val orientation = geospatialPose.eastUpSouthQuaternion
                    val projectedLocation = GeometryEngine.projectOrNull(
                        Point(
                            geospatialPose.longitude,
                            geospatialPose.latitude,
                            geospatialPose.altitude + calibrationState.totalElevationOffset,
                            SpatialReference(
                                SpatialReference.wgs84().wkid,
                                115700 /*WGS84_VERTICAL*/
                            )
                        ),
                        SpatialReference(
                            SpatialReference.wgs84().wkid,
                            verticalWkid = 5773 /*EGM96*/
                        )
                    ) ?: return@let

                    cameraController.setOriginCamera(
                        Camera(
                            projectedLocation,
                            0.0,
                            90.0,
                            0.0
                        )
                    )
                    val original = earth.getPose(
                        geospatialPose.latitude,
                        geospatialPose.longitude,
                        geospatialPose.altitude,
                        orientation[0],
                        orientation[1],
                        orientation[2],
                        orientation[3]
                    )

                    val physicalPose = when (display?.rotation ?: 0) {
                        Surface.ROTATION_90 -> original.compose(
                            Pose.makeRotation(
                                0f,
                                0f,
                                -sqrt(0.5).toFloat(),
                                sqrt(0.5).toFloat()
                            )
                        )

                        Surface.ROTATION_180 -> original.compose(
                            Pose.makeRotation(
                                0f,
                                0f,
                                sqrt(1.0).toFloat(),
                                0f
                            )
                        )

                        Surface.ROTATION_270 -> original.compose(
                            Pose.makeRotation(
                                0f,
                                0f,
                                sqrt(0.5).toFloat(),
                                sqrt(0.5).toFloat()
                            )
                        )

                        else -> original
                    }

                    hasSetOriginCamera = true
                    cameraController.transformationMatrix =
                        TransformationMatrix.createWithQuaternionAndTranslation(
                            physicalPose.qx().toDouble(),
                            physicalPose.qy().toDouble(),
                            physicalPose.qz().toDouble(),
                            physicalPose.qw().toDouble(),
                            0.0,
                            0.0,
                            0.0
                        )
                } catch (e: Throwable) {
                    Log.e("GeospatialTrackingCameraController", "Failed to update camera", e)
                    // Ignore
                }
            }
        }
    }
//}

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
