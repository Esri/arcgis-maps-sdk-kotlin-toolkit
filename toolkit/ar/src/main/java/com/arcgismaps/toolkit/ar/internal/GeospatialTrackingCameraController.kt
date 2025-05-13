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
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.toolkit.ar.ArCoreAuthorizationException
import com.arcgismaps.toolkit.ar.ArCoreResourceExhaustedException
import com.google.ar.core.Earth
import com.google.ar.core.Earth.EarthState
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotTrackingException
import kotlin.math.sqrt

/**
 * Wraps a [TransformationMatrixCameraController] and uses Google's Geospatial API to update the camera's position.
 *
 * @see updateCamera to update the camera using the [Earth.getCameraGeospatialPose].
 *
 * @since 200.7.0
 */
internal class GeospatialTrackingCameraController(
    private val calibrationState: CalibrationState,
    clippingDistance: Double?,
    context: Context,
    private val onError: (error: Throwable?, hasSetOriginCamera: Boolean) -> Unit
) : WorldScaleCameraController {
    override val cameraController = TransformationMatrixCameraController().apply {
        this.clippingDistance = clippingDistance
    }
    override var hasSetOriginCamera: Boolean by mutableStateOf(false)
        private set

    @Suppress("DEPRECATION")
    private val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
    }

    private var error: Throwable? = null

    override fun updateCamera(frame: Frame, session: Session) {
        session.earth?.let { earth ->
            checkForEarthStateErrors(earth, hasSetOriginCamera)
            if (error != null) return@let
            if (earth.trackingState != TrackingState.TRACKING) return@let
            if (earth.earthState != EarthState.ENABLED) return@let

            val geospatialPose = earth.cameraGeospatialPose
            val geospatialOrientation = geospatialPose.eastUpSouthQuaternion
            // The scene camera is expected to be positioned based on orthometric height but the geospatial pose
            // gives us ellipsoidal heights. We need to project vertically to get a correct height for the scene camera.
            val projectedLocation = GeometryEngine.projectOrNull(
                Point(
                    geospatialPose.longitude,
                    geospatialPose.latitude,
                    geospatialPose.altitude + calibrationState.totalElevationOffset,
                    WorldScaleParameters.SR_WGS84_WGS_VERTICAL
                ),
                WorldScaleParameters.SR_CAMERA
            ) ?: return@let


            // get a pose relative to local coordinates so we can rotate the orientation relative
            // to the device orientation
            val localPose = try {
                earth.getPose(
                    geospatialPose.latitude,
                    geospatialPose.longitude,
                    geospatialPose.altitude,
                    geospatialOrientation[0],
                    geospatialOrientation[1],
                    geospatialOrientation[2],
                    geospatialOrientation[3]
                )
            } catch (e: NotTrackingException) {
                // Even though we check for tracking state above, sometimes it can still be not tracking
                // when we try to get the pose.
                return@let
            }

            // set the origin camera based on lat, lon and altitude of the geospatial pose
            cameraController.setOriginCamera(
                Camera(
                    projectedLocation,
                    calibrationState.totalHeadingOffset,
                    90.0,
                    0.0
                )
            )

            // rotate the local pose based on the display orientation,
            // then convert it back to a geospatial pose
            val displayOrientedGeospatialOrientation = when (display?.rotation ?: 0) {
                Surface.ROTATION_90 ->
                    localPose.compose(POSE_ROTATION_90).let {
                        earth.getGeospatialPose(it).eastUpSouthQuaternion
                    }

                Surface.ROTATION_180 ->
                    localPose.compose(POSE_ROTATION_180).let {
                        earth.getGeospatialPose(it).eastUpSouthQuaternion
                    }

                Surface.ROTATION_270 ->
                    localPose.compose(POSE_ROTATION_270).let {
                        earth.getGeospatialPose(it).eastUpSouthQuaternion
                    }

                else -> {
                    // in normal portrait we don't need to adjust for display orientation
                    geospatialOrientation
                }
            }

            hasSetOriginCamera = true
            cameraController.transformationMatrix =
                TransformationMatrix.createWithQuaternionAndTranslation(
                    displayOrientedGeospatialOrientation[0].toDouble(),
                    displayOrientedGeospatialOrientation[1].toDouble(),
                    displayOrientedGeospatialOrientation[2].toDouble(),
                    displayOrientedGeospatialOrientation[3].toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
        }
    }

    /**
     * Checks the [EarthState] of the [Earth] object and sets the [error] if there is an error.
     *
     * This should be called after the [Earth] object is created and before the camera is set.
     *
     * @since 200.7.0
     */
    private fun checkForEarthStateErrors(earth: Earth, hasSetOriginCamera: Boolean) {
        if (earth.earthState != EarthState.ENABLED && error != null) {
            // if we are in an error state and the earth state is not enabled,
            // then don't do anything.
            // This prevents us from changing an error that might already exist and propagating that
            // to the user, but it's probably better than propagating a new error every frame even
            // if it hasn't changed.
            return
        }
        when (earth.earthState) {
            EarthState.ENABLED -> {
                if (error != null) {
                    onError(null, hasSetOriginCamera)
                    error = null
                }
            }
            EarthState.ERROR_INTERNAL, EarthState.ERROR_GEOSPATIAL_MODE_DISABLED -> {
                error = IllegalStateException(
                    "WorldScaleSceneView has encountered an internal error. The app should not attempt to recover from this error. Please see the Android logs for additional information."
                ).also {
                    onError(it, hasSetOriginCamera)
                }
            }

            EarthState.ERROR_NOT_AUTHORIZED -> {
                error = ArCoreAuthorizationException().also {
                    onError(it, hasSetOriginCamera)
                }
            }

            EarthState.ERROR_RESOURCE_EXHAUSTED -> {
                error = ArCoreResourceExhaustedException().also {
                    onError(it, hasSetOriginCamera)
                }
            }

            EarthState.ERROR_APK_VERSION_TOO_OLD -> {
                error = IllegalStateException(
                    "The ARCore APK is older than the current supported version."
                ).also {
                    onError(it, hasSetOriginCamera)
                }
            }
        }
    }

    companion object {
        private val POSE_ROTATION_90 = Pose.makeRotation(
            0f,
            0f,
            -sqrt(0.5).toFloat(),
            sqrt(0.5).toFloat()
        )

        private val POSE_ROTATION_180 = Pose.makeRotation(
            0f,
            0f,
            sqrt(1.0).toFloat(),
            0f
        )

        private val POSE_ROTATION_270 = Pose.makeRotation(
            0f,
            0f,
            sqrt(0.5).toFloat(),
            sqrt(0.5).toFloat()
        )
    }
}
