package com.arcgismaps.toolkit.ar.internal

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
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
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.ByteArrayOutputStream

/**
 * State that has been derived/calculated from a [Frame] such as the [projectedLocation] of
 * the frame's camera. This state can be used by various components during a frame update.
 * It avoids repeated calculations of the same state throughout a frame update.
 */
internal class FrameState(
    val frame: Frame,
    val session: Session,
    private val calibrationState: CalibrationState
) {

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

    /**
     * The camera image bytes in JPG format.
     */
    lateinit var cameraImageBytes: ByteArray
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

        cameraImageBytes = try {
            frame.acquireCameraImage().use {
                it.toJpgBytes()
            }
        } catch (_: NotYetAvailableException) {
            // Sometimes the camera image is not yet available, so we just return an empty byte array in that case.
            ByteArray(0)
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

private fun Image.toJpgBytes(jpegQuality: Int = 100): ByteArray {
    require(format == ImageFormat.YUV_420_888) {
        "Expected YUV_420_888, got format=${format}"
    }

    val nv21 = yuv420888ToNv21()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return ByteArrayOutputStream().use { out ->
        yuvImage.compressToJpeg(Rect(0, 0, width, height), jpegQuality, out)
        out.toByteArray()
    }
}

/**
 * Converts android.media.Image in YUV_420_888 to NV21.
 * Handles arbitrary rowStride/pixelStride from camera drivers.
 */
private fun Image.yuv420888ToNv21(): ByteArray {
    require(format == ImageFormat.YUV_420_888) {
        "Expected YUV_420_888, got format=$format"
    }

    val imageWidth = width
    val imageHeight = height
    val ySize = imageWidth * imageHeight
    val uvSize = imageWidth * imageHeight / 2
    val nv21 = ByteArray(ySize + uvSize)

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()

    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride

    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride

    var dstIndex = 0

    // Copy Y plane
    for (row in 0 until imageHeight) {
        val rowStart = row * yRowStride
        for (col in 0 until imageWidth) {
            val index = rowStart + col * yPixelStride
            require(index < yBuffer.limit()) {
                "Y plane index out of bounds: index=$index limit=${yBuffer.limit()} row=$row col=$col rowStride=$yRowStride pixelStride=$yPixelStride"
            }
            nv21[dstIndex++] = yBuffer.get(index)
        }
    }

    // Copy UV planes into NV21 layout: VU VU VU...
    val uvWidth = imageWidth / 2
    val uvHeight = imageHeight / 2

    for (row in 0 until uvHeight) {
        val uRowStart = row * uRowStride
        val vRowStart = row * vRowStride
        for (col in 0 until uvWidth) {
            val uIndex = uRowStart + col * uPixelStride
            val vIndex = vRowStart + col * vPixelStride

            require(uIndex < uBuffer.limit()) {
                "U plane index out of bounds: index=$uIndex limit=${uBuffer.limit()} row=$row col=$col rowStride=$uRowStride pixelStride=$uPixelStride"
            }
            require(vIndex < vBuffer.limit()) {
                "V plane index out of bounds: index=$vIndex limit=${vBuffer.limit()} row=$row col=$col rowStride=$vRowStride pixelStride=$vPixelStride"
            }

            nv21[dstIndex++] = vBuffer.get(vIndex)
            nv21[dstIndex++] = uBuffer.get(uIndex)
        }
    }

    return nv21
}

