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
    private val onError: (Throwable) -> Unit
) : WorldScaleCameraController {
    override val cameraController = TransformationMatrixCameraController().apply {
        this.clippingDistance = clippingDistance
    }
    override var hasSetOriginCamera: Boolean by mutableStateOf(false)
        private set

    private val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
    }

    private var lastEarthState: EarthState? = null

    override fun updateCamera(frame: Frame, session: Session) {
        session.earth?.let { earth ->
            when (earth.earthState) {
                EarthState.ENABLED -> {}
                EarthState.ERROR_INTERNAL, EarthState.ERROR_GEOSPATIAL_MODE_DISABLED -> {
                    onError(
                        IllegalStateException(
                            "WorldScaleSceneView has encountered an internal error. The app should not attempt to recover from this error. Please see the Android logs for additional information."
                        )
                    )
                }

                EarthState.ERROR_NOT_AUTHORIZED -> {
                    onError(
                        IllegalStateException(
                            """
                                The Google Cloud authorization provided by the application is not valid.
                                - The associated Google Cloud project may not have enabled the ARCore API.
                                - When using API key authentication, this will happen if the API key in the manifest is invalid or unauthorized. It may also fail if the API key is restricted to a set of apps not including the current one.
                                - When using keyless authentication, this may happen when no OAuth client has been created, or when the signing key and package name combination does not match the values used in the Google Cloud project. It may also fail if Google Play Services isn't installed, is too old, or is malfunctioning for some reason (e. g. killed due to memory pressure).
                                    """.trimIndent()
                        )
                    )
                }

                EarthState.ERROR_RESOURCE_EXHAUSTED -> {
                    onError(
                        IllegalStateException(
                            "The application has exhausted the quota allotted to the given Google Cloud project. The developer should request additional quota for the ARCore API for their project from the Google Cloud Console."
                        )
                    )
                }

                EarthState.ERROR_APK_VERSION_TOO_OLD -> {
                    onError(
                        IllegalStateException(
                            "The ARCore APK is older than the current supported version."
                        )
                    )
                }
            }
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
