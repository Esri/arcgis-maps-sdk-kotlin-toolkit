package com.arcgismaps.toolkit.arrealitycaptureapp.screens

import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.util.SizeF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolMarkerPlacement
import com.arcgismaps.mapping.symbology.SimpleLineSymbolMarkerStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.ArOrientedImage
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.arrealitycaptureapp.io.CamerasTable_CAMERA_ID
import com.arcgismaps.toolkit.arrealitycaptureapp.io.CamerasTable_OBJECT_ID
import com.arcgismaps.toolkit.arrealitycaptureapp.io.FrameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val TAG = "RealityCaptureApp"

class MainScreenViewModel(application: Application): AndroidViewModel(application) {
    val frameRepository by lazy { FrameRepository(application) }

    val proxy = WorldScaleSceneViewProxy()

    val capturedImages = mutableMapOf<String, ByteArray>()

    // The graphics overlay should have a surface placement of absolute to ensure
    // that graphics are placed correctly in 3D space if the user taps on a real object in the camera
    // feed, such as a wall or tree
    val graphicsOverlays = listOf(
            GraphicsOverlay().apply {
                sceneProperties.surfacePlacement = SurfacePlacement.Absolute
            }
        )

    private var camerasTableInitialized = false
    private var currentFrameObjectId = 0

    fun captureOrientedImage() {
        viewModelScope.launch(Dispatchers.IO) {
            proxy.exportOrientedImage()?.let {
                // populate the cameras table with the camera parameters only once, since they are the same for all frames
                if (!camerasTableInitialized) {
                    camerasTableInitialized = true
                    frameRepository.appendCamera(
                        objectId = CamerasTable_OBJECT_ID,
                        cameraId = CamerasTable_CAMERA_ID,
                        focalLength = it.focalLength.toMicrons(application.pixelPitch(it.cameraId)), // in microns
                        pixelSize = application.sensorSize(it.cameraId).toDouble() // in microns
                    )
                }

                val imgFileName = "$currentFrameObjectId.jpg"
                capturedImages[imgFileName] = it.cameraImageBytes

                // populate the frames table with the captured oriented image
                frameRepository.appendFrame(
                    objectId = currentFrameObjectId,
                    raster = imgFileName,
                    cameraId = CamerasTable_CAMERA_ID,
                    perspectiveX = it.geospatialPose.longitude, // TODO - project from geographic to projected, add SRS to camera table
                    perspectiveY = it.geospatialPose.latitude, // TODO - project from geographic to projected
                    perspectiveZ = it.geospatialPose.altitude, /* orthometric height */
                    omega = it.cameraRotationAngles.omegaDeg,
                    phi = it.cameraRotationAngles.phiDeg,
                    kappa = it.cameraRotationAngles.kappaDeg
                )
                currentFrameObjectId++

                graphicsOverlays.first().addFrustumGraphic(it)
            } ?: Log.d(TAG, "Failed to capture oriented image.")
        }
    }

    fun saveCaptureSession() {
        val rootDir = application.getExternalFilesDir(null) ?: throw IllegalStateException("External files directory is not available.")
        val now = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        )
        val dir = File(rootDir, "realitycapture/$now")

        frameRepository.saveToCsv(dir)
        saveCapturedImages(dir)
    }

    private fun saveCapturedImages(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            capturedImages.forEach { (fileName, image) ->
                val outputFile = File(directory, fileName)
                FileOutputStream(outputFile).use {
                    it.write(image)
                }
            }
            capturedImages.clear() // clear the map after saving to free up memory
        }
    }
}

/**
 * Gets the camera sensor size in microns.
 */
private fun Context.sensorSize(cameraId: String): Float {
    val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val chars = cm.getCameraCharacteristics(cameraId)

    val physicalSize: SizeF =
        chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: throw IllegalStateException("Camera characteristics not found for cameraId: $cameraId")

    // TODO: x vs y ??
    return physicalSize.width * 1000f // convert mm to microns
}

/**
 * Gets the pixel pitch in microns/px.
 */
private fun Context.pixelPitch(cameraId: String): Double {
    val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val chars = cm.getCameraCharacteristics(cameraId)

    val physicalSize: SizeF =
        chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: throw IllegalStateException("Camera characteristics not found for cameraId: $cameraId")

    // Prefer full pixel array; fallback to active array.
    val pixelArraySize: Size? =
        chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

    val activeRect: Rect? =
        chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

    val pixelWidth: Int
    val pixelHeight: Int
    if (pixelArraySize != null) {
        pixelWidth = pixelArraySize.width
        pixelHeight = pixelArraySize.height
    } else if (activeRect != null) {
        pixelWidth = activeRect.width()
        pixelHeight = activeRect.height()
    } else {
        throw IllegalStateException("Camera characteristics do not contain pixel array size or active array size for cameraId: $cameraId")
    }

    val pitchXUm = (physicalSize.width.toDouble() * 1000.0) / pixelWidth.toDouble()
    val pitchYUm = (physicalSize.height.toDouble() * 1000.0) / pixelHeight.toDouble()

    // TODO pitchXUm vs pitchYUm ??
    return pitchXUm
}

private fun FloatArray.toMicrons(pixelPitch: Double): Double {
    val fxUm = this[0] * pixelPitch
    val fyUm = this[1] * pixelPitch

    // TODO: fxUm vs fyUm ??
    return fxUm
}

private fun GraphicsOverlay.addFrustumGraphic(orientedImage: ArOrientedImage) {

    // graphic for camera location
    Graphic(
        geometry = orientedImage.location,
        symbol = SimpleMarkerSceneSymbol(
            style = SimpleMarkerSceneSymbolStyle.Sphere,
            color = Color.red,
            height = 0.1,
            width = 0.1,
            depth = 0.1,
            anchorPosition = SceneSymbolAnchorPosition.Center
        )
    ).also {
        this.graphics.add(it)
    }

    // graphic for camera orientation
    orientedImage.orientationPointer?.let { pointer ->
        Graphic(
            geometry = pointer,
            symbol = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Solid,
                color = Color.green,
                width = 2f,
                markerStyle = SimpleLineSymbolMarkerStyle.Arrow,
                markerPlacement = SimpleLineSymbolMarkerPlacement.End
            )
        ).also {
            this.graphics.add(it)
        }
    }
}
