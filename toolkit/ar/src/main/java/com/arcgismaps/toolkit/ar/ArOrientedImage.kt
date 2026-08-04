package com.arcgismaps.toolkit.ar

import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.toolkit.ar.internal.FrameState
import com.arcgismaps.toolkit.ar.internal.WorldScaleParameters
import com.google.ar.core.Pose

public class ArOrientedImage internal constructor(private val frame: FrameState) {
    public val location: Point by lazy {
        frame.projectedLocation
    }

    public val orientationPointer: Polyline? by lazy {
        orientationPointer()
    }

    /**
     * The focal length of the camera in pixels.
     */
    public val focalLength: FloatArray by lazy {
        frame.frame.camera.imageIntrinsics.focalLength
    }

    /**
     * Returns a FloatArray[2] containing the principal point.
     * The order of values is {cx, cy}, in pixels.
     */
    public val principalPoint: FloatArray by lazy {
        frame.frame.camera.imageIntrinsics.principalPoint
    }

    public val cameraId: String by lazy {
        frame.session.cameraConfig.cameraId
    }

    private fun orientationPointer(): Polyline? =
        frame.localPose.moveForward(0.5f)?.let {
            Polyline(points = listOf(location, it))
        }

    private fun Pose.moveForward(distanceInMeters: Float): Point? {
        val earth = frame.session.earth ?: return null

        // ARCore local coordinates: +X right, +Y up, +Z backward.
        // "Forward" is therefore -Z.
        val translation = Pose.makeTranslation(0f, 0f, -distanceInMeters)

        // Apply forward translation relative to receiver local pose orientation.
        val translatedLocalPose = this.compose(translation)

        // Convert translated local pose into geospatial/world pose.
        val translatedGeoPose = earth.getGeospatialPose(translatedLocalPose)

        // Build WGS84(+vertical) point, then project to world/camera SR used by toolkit.
        val wgsPoint = Point(
            translatedGeoPose.longitude,
            translatedGeoPose.latitude,
            translatedGeoPose.altitude,
            WorldScaleParameters.SR_WGS84_WGS_VERTICAL
        )

        return GeometryEngine.projectOrNull(wgsPoint, WorldScaleParameters.SR_CAMERA)
    }
}
