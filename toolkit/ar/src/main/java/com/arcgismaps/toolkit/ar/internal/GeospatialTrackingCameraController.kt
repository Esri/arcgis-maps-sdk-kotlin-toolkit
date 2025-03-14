package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.content.Context.DISPLAY_SERVICE
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose
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
//                val mappedOrientation = FloatArray(9)
//                SensorManager.remapCoordinateSystem(orientation, SensorManager.AXIS_X, SensorManager.AXIS_Z, mappedOrientation)
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

                val displayManager: DisplayManager = ArcGISEnvironment.applicationContext!!.getSystemService(DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                when(display.rotation) {
                    Surface.ROTATION_0 -> {
                        Log.d("RotationTest", "Surface.ROTATION_0 ${orientation[0]}, ${orientation[1]}, ${orientation[2]}, ${orientation[3]}")
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
                    Surface.ROTATION_90 -> {
                        Log.d("RotationTest", "Surface.ROTATION_90 ${orientation[0]}, ${orientation[1]}, ${orientation[2]}, ${orientation[3]}")
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
                    Surface.ROTATION_180 -> {
                        Log.d("RotationTest", "Surface.ROTATION_180 ${orientation[0]}, ${orientation[1]}, ${orientation[2]}, ${orientation[3]}")
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
                    Surface.ROTATION_270 -> {
                        Log.d("RotationTest", "Surface.ROTATION_270 ${orientation[0]}, ${orientation[1]}, ${orientation[2]}, ${orientation[3]}")
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
