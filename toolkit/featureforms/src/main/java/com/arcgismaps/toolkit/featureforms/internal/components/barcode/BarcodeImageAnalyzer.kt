/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.time.Instant

internal data class BarcodeInfo(
    val boundingBox: Rect?,
    val rawValue: String
)

/**
 * An [ImageAnalysis.Analyzer] that processes images from the camera preview to detect barcodes.
 *
 * @param frame The frame in which the barcode should be detected. This should be in the view
 * coordinate system.
 * @param onSuccess The callback that is called when a barcode is detected. The first parameter is
 * the bounding box of the barcode in the view coordinate system, and the second parameter is the
 * raw value of the barcode.
 */
internal class BarcodeImageAnalyzer(
    private val frame: Rect,
    private val onSuccess: (List<BarcodeInfo>) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * The last time a frame was processed.
     */
    private var lastAnalyzedTimestamp = Instant.now()

    // set by updateTransform
    private var sensorToTargetMatrix: Matrix? = null

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .enableAllPotentialBarcodes()
            .build()
    )

    override fun getTargetCoordinateSystem(): Int {
        // set the target coordinate system to view referenced. This will allow the analyzer to
        // return the appropriate transformation matrix in updateTransform()
        return ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        // using bitmap here is inefficient but ImageProxy.getImage() is experimental
        image.image?.let {
            barcodeScanner.process(it, image.imageInfo.rotationDegrees)
                .addOnSuccessListener { barcodes ->
                    processBarcodes(barcodes, image)
                }.addOnFailureListener {
                    image.close()
                }
        }
    }

    override fun updateTransform(matrix: Matrix?) {
        sensorToTargetMatrix = matrix
    }

    /**
     * Processes the list of [Barcode]s and returns the first barcode that has a non-empty raw value.
     */
    private fun processBarcodes(barcodes: List<Barcode>, image: ImageProxy) {
        //Log.e("TAG", "processBarcodes: res -${image.width}x${image.height}")
        val barcodesInfo = mutableListOf<BarcodeInfo>()
        barcodes.forEach { barcode ->
            // filter out barcodes that do not have a raw value or the raw value is empty
            if (barcode.rawValue == null || barcode.rawValue!!.isEmpty()) {
                return@forEach
            }
            // Get the bounding box of the barcode and convert it to the view coordinate system.
            val rect = barcode.boundingBox?.let { box ->
                getTransformationMatrix(image)?.let { matrix ->
                    val sourcePoints = floatArrayOf(
                        box.left.toFloat(),
                        box.top.toFloat(),
                        box.right.toFloat(),
                        box.top.toFloat(),
                        box.right.toFloat(),
                        box.bottom.toFloat(),
                        box.left.toFloat(),
                        box.bottom.toFloat()
                    )
                    // Convert the bounding box to the view coordinate system using the
                    // transformation matrix.
                    matrix.mapPoints(sourcePoints)
                    Rect(
                        Offset(sourcePoints[0], sourcePoints[1]),
                        Offset(sourcePoints[4], sourcePoints[5])
                    )
                }
            }
            if (rect != null) {
                // If the barcode has a bounding box, check if it is inside the frame.
                if (rect.isRectInside(frame)) {
                    barcodesInfo.add(BarcodeInfo(rect, barcode.rawValue!!))
                }
            } else {
                // If the barcode does not have a bounding box, return the raw value.
                barcodesInfo.add(BarcodeInfo(null, barcode.rawValue!!))
            }
        }
        onSuccess(barcodesInfo)
        image.close()
    }

    /**
     * Calculates the transformation matrix from the image coordinate system in [imageProxy] to the
     * view coordinate system.
     *
     * Taken from :
     * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/camera/camera-mlkit-vision/src/main/java/androidx/camera/mlkit/vision/MlKitAnalyzer.java#164
     */
    private fun getTransformationMatrix(imageProxy: ImageProxy): Matrix? {
        val analysisToTarget = Matrix()
        if (sensorToTargetMatrix == null) {
            imageProxy.close()
            return null
        }
        val sensorToAnalysis = Matrix(imageProxy.imageInfo.sensorToBufferTransformMatrix)
        // Calculate the rotation added by ML Kit.
        val sourceRect = RectF(
            0f, 0f, imageProxy.width.toFloat(),
            imageProxy.height.toFloat()
        )
        val bufferRect = TransformUtils.rotateRect(
            sourceRect,
            imageProxy.imageInfo.rotationDegrees
        )
        val analysisToMlKitRotation = TransformUtils.getRectToRect(
            sourceRect,
            bufferRect,
            imageProxy.imageInfo.rotationDegrees
        )
        // Concat the MLKit transformation with sensor to Analysis.
        sensorToAnalysis.postConcat(analysisToMlKitRotation)
        // Invert to get analysis to sensor.
        sensorToAnalysis.invert(analysisToTarget)
        // Concat the sensor to target transformation to get the overall transformation.
        analysisToTarget.postConcat(sensorToTargetMatrix)
        return analysisToTarget
    }
}

/**
 * Checks if the [Rect] is inside the [other] [Rect]. Returns true if all the corners of the [Rect]
 * are inside the [other] [Rect].
 */
internal fun Rect.isRectInside(other: Rect): Boolean {
    return other.contains(this.topLeft) &&
        other.contains(this.topRight) &&
        other.contains(this.bottomLeft) &&
        other.contains(this.bottomRight)
}
