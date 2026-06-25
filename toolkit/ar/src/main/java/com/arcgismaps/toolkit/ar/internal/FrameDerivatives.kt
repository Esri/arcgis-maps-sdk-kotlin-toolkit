package com.arcgismaps.toolkit.ar.internal

import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.ar.ArCoreAuthorizationException
import com.arcgismaps.toolkit.ar.ArCoreResourceExhaustedException
import com.google.ar.core.Earth
import com.google.ar.core.Earth.EarthState
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotTrackingException

/**
 * Frame state that has been derived/calculated from a [Frame]. This state can be used by various
 * components during a frame update.
 */
internal class FrameDerivatives(
    val frame: Frame,
    val session: Session,
    private val calibrationState: CalibrationState) {

    init {
        initialize()
    }

    var error: Throwable? = null
        private set

    lateinit var geospatialPose: GeospatialPose
        private set

    lateinit var projectedLocation: Point
        private set

    lateinit var localPose: Pose
        private set

    private fun initialize() {
        session.earth?.let { earth ->
            checkForEarthStateErrors(earth)
            if (error != null) return@let
            if (earth.trackingState != TrackingState.TRACKING) return@let
            if (earth.earthState != EarthState.ENABLED) return@let

            geospatialPose = earth.cameraGeospatialPose
            val geospatialOrientation = geospatialPose.eastUpSouthQuaternion
            // The scene camera is expected to be positioned based on orthometric height but the geospatial pose
            // gives us ellipsoidal heights. We need to project vertically to get a correct height for the scene camera.
            projectedLocation = GeometryEngine.projectOrNull(
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
            localPose = try {
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
        }
    }

    private fun checkForEarthStateErrors(earth: Earth) {
        when (earth.earthState) {
            EarthState.ENABLED -> {
                // all good, nothing to do...
            }

            EarthState.ERROR_INTERNAL, EarthState.ERROR_GEOSPATIAL_MODE_DISABLED -> {
                error = IllegalStateException(
                    "WorldScaleSceneView has encountered an internal error. The app should not attempt to recover from this error. Please see the Android logs for additional information."
                )
            }

            EarthState.ERROR_NOT_AUTHORIZED -> {
                error = ArCoreAuthorizationException()
            }

            EarthState.ERROR_RESOURCE_EXHAUSTED -> {
                error = ArCoreResourceExhaustedException()
            }

            EarthState.ERROR_APK_VERSION_TOO_OLD -> {
                error = IllegalStateException(
                    "The ARCore APK is older than the current supported version."
                )
            }
        }
    }
}
