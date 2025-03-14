package com.arcgismaps.toolkit.ar.internal

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlin.math.asin
import kotlin.math.atan2

internal class GeospatialTrackingCameraController(
    private val calibrationState: CalibrationState,
    private val sessionWrapper: ArSessionWrapper
) : WorldScaleCameraController {
    override val cameraController = TransformationMatrixCameraController()
    override var hasSetOriginCamera: Boolean by mutableStateOf(false)
        private set

    override fun updateCamera(frame: Frame) {
        sessionWrapper.withLock { session, _ ->
//            Log.d("GeospatialTrackingCameraController", "updateCamera")
            session.earth?.let {  earth ->
                if (earth.trackingState != TrackingState.TRACKING) return@let
                if (earth.earthState != Earth.EarthState.ENABLED) return@let
                val pose = earth.cameraGeospatialPose
                val orientation = pose.eastUpSouthQuaternion
                val projectedLocation = GeometryEngine.projectOrNull(
                    Point(
                        pose.longitude,
                        pose.latitude,
                        pose.altitude + calibrationState.totalElevationOffset,
                        SpatialReference(SpatialReference.wgs84().wkid, 115700 /*WGS84_VERTICAL*/)
                    ),
                    SpatialReference(SpatialReference.wgs84().wkid, verticalWkid = 5773 /*EGM96*/)
                ) ?: return@let

                cameraController.setOriginCamera(
                    Camera(
                        projectedLocation,
                        calibrationState.totalHeadingOffset,
                        90.0,
                        0.0
                    )
                )
                hasSetOriginCamera = true
                cameraController.transformationMatrix = TransformationMatrix.createWithQuaternionAndTranslation(
                    orientation[0].toDouble(),
                    orientation[1].toDouble(),
                    orientation[2].toDouble(),
                    orientation[3].toDouble(),
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
    }
}

@Composable
internal fun rememberGeospatialTrackingCameraController(
    calibrationState: CalibrationState,
    sessionWrapper: ArSessionWrapper
): WorldScaleCameraController {
    sessionWrapper.onResume(LocalLifecycleOwner.current)
    return remember { GeospatialTrackingCameraController(calibrationState, sessionWrapper) }
}
