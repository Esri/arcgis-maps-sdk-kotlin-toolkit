package com.arcgismaps.toolkit.ar

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DeviceOrientation
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class TableTopSceneViewState internal constructor(
    arcGISSceneAnchor: Point,
    clippingDistance: Double,
    translationFactor: Double,
    private val tableTopSceneViewProxy: TableTopSceneViewProxy
) {
    val cameraController = TransformationMatrixCameraController().apply {
        this.clippingDistance = clippingDistance
        setTranslationFactor(translationFactor)
        setOriginCamera(Camera(arcGISSceneAnchor, 0.0, 90.0, 0.0))
    }


    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val identityMatrix = TransformationMatrix.createIdentityMatrix()
    private var anchor: Anchor? = null

    fun onTap(hit: HitResult?) {
        hit?.let { hitResult ->
            if (!_ready.value) {
                anchor = hitResult.createAnchor()
                _ready.value = true
            }
        }
    }

    fun onFrame(frame: Frame) {
        anchor?.let { anchor ->
            val anchorPosition = identityMatrix - anchor.pose.translation.let {
                TransformationMatrix.createWithQuaternionAndTranslation(
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    it[0].toDouble(),
                    it[1].toDouble(),
                    it[2].toDouble()
                )
            }
            val cameraPosition =
                anchorPosition + frame.camera.displayOrientedPose.transformationMatrix
            cameraController.transformationMatrix = cameraPosition
            val imageIntrinsics = frame.camera.imageIntrinsics
            tableTopSceneViewProxy.sceneViewProxy.setFieldOfViewFromLensIntrinsics(
                imageIntrinsics.focalLength[0],
                imageIntrinsics.focalLength[1],
                imageIntrinsics.principalPoint[0],
                imageIntrinsics.principalPoint[1],
                imageIntrinsics.imageDimensions[0].toFloat(),
                imageIntrinsics.imageDimensions[1].toFloat(),
                // TODO:
                deviceOrientation = DeviceOrientation.Portrait
            )
            tableTopSceneViewProxy.sceneViewProxy.renderFrame()
        }
    }
}
