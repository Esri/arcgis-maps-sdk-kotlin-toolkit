package com.arcgismaps.toolkit.ar

import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.toolkit.ar.internal.FrameState
import com.arcgismaps.toolkit.ar.internal.WorldScaleParameters
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Pose
import kotlin.math.asin
import kotlin.math.atan2

public class ArOrientedImage internal constructor(private val frame: FrameState) {
    public val location: Point by lazy {
        frame.projectedLocation
    }

    public val geospatialPose: GeospatialPose by lazy {
        frame.geospatialPose
    }

    /**
     * Heading. pitch and roll of the camera in degrees, relative to the WGS84 coordinate system.
     */
    public val cameraRotationAngles: Opk by lazy {
        arcoreGeospatialToOpk(frame.geospatialPose.eastUpSouthQuaternion)
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

    public val cameraImageBytes: ByteArray
        get() = frame.cameraImageBytes

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

/**
 * Rotational camera angles Omega, Phi, and Kappa in degrees.
 */
public data class Opk(val omegaDeg: Double, val phiDeg: Double, val kappaDeg: Double)

private data class Mat3(val m: Array<DoubleArray>) // m[row][col]

private fun quatToMat3(qx: Double, qy: Double, qz: Double, qw: Double): Mat3 {
    val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
    val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
    val wx = qw * qx; val wy = qw * qy; val wz = qw * qz
    return Mat3(arrayOf(
        doubleArrayOf(1 - 2*(yy + zz), 2*(xy - wz),     2*(xz + wy)),
        doubleArrayOf(2*(xy + wz),     1 - 2*(xx + zz), 2*(yz - wx)),
        doubleArrayOf(2*(xz - wy),     2*(yz + wx),     1 - 2*(xx + yy))
    ))
}

private fun mul(a: Mat3, b: Mat3): Mat3 {
    val r = Array(3) { DoubleArray(3) }
    for (i in 0..2) for (j in 0..2) {
        r[i][j] = 0.0
        for (k in 0..2) r[i][j] += a.m[i][k] * b.m[k][j]
    }
    return Mat3(r)
}

private fun radToDeg(x: Double) = x * 180.0 / Math.PI

/**
 * Converts ARCore geospatial pose quaternion to OPK angles in degrees.
 *
 * @param eusQ The east-up-south quaternion from ARCore geospatial pose. Represents the camera/device
 * orientation in the local geospatial EUS frame, where `+X = East`, `+Y = Up`, and `+Z = South`.
 * The array contains the quaternion components `[x, y, z, w]`.
 *
 * @return An Opk object containing omega, phi, and kappa angles in degrees.
 */
private fun arcoreGeospatialToOpk(eusQ: FloatArray): Opk {
    // convert quaternion to rotation matrix
    val rCamToEus = quatToMat3(
        eusQ[0].toDouble(), eusQ[1].toDouble(), eusQ[2].toDouble(), eusQ[3].toDouble()
    )

    // EUS (ast-Up-South frame) to ENU (East-North-Up frame) rotation matrix
    val rEusToEnu = Mat3(arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0,-1.0),
        doubleArrayOf(0.0, 1.0, 0.0)
    ))

    // Camera convention correction (adjust if needed for your Esri export)
    val f = Mat3(arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0,-1.0, 0.0),
        doubleArrayOf(0.0, 0.0,-1.0)
    ))

    val r = mul(mul(rEusToEnu, rCamToEus), f)

    val phi = asin(-r.m[2][0])
    val omega = atan2(r.m[2][1], r.m[2][2])
    val kappa = atan2(r.m[1][0], r.m[0][0])

    return Opk(radToDeg(omega), radToDeg(phi), radToDeg(kappa))
}