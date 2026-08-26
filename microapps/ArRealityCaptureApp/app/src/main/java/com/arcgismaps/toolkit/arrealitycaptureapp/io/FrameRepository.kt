package com.arcgismaps.toolkit.arrealitycaptureapp.io

import android.content.Context
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import java.io.File

class FrameRepository(private val context: Context) {

    private var framesTable = emptyDataFrame<FramesTable>()
    private var camerasTable = emptyDataFrame<CamerasTable>()
    private var orientedImageryTable = emptyDataFrame<OrientedImageryTable>()

    fun appendFrame(
        objectId: Int,
        raster: String,
        cameraId: String,
        perspectiveX: Double,
        perspectiveY: Double,
        perspectiveZ: Double,
        omega: Double,
        phi: Double,
        kappa: Double
    ) {
        val newRow = dataFrameOf(
            "ObjectID" to listOf(objectId),
            "Raster" to listOf(raster),
            "CameraID" to listOf(cameraId),
            "PerspectiveX" to listOf(perspectiveX),
            "PerspectiveY" to listOf(perspectiveY),
            "PerspectiveZ" to listOf(perspectiveZ),
            "Omega" to listOf(omega),
            "Phi" to listOf(phi),
            "Kappa" to listOf(kappa)
        ).cast<FramesTable>()
        framesTable = framesTable.concat(newRow)
    }

    fun appendCamera(
        objectId: Int,
        cameraId: String,
        focalLength: Double,
        pixelSize: Double,
        srs: String
    ) {
        val newRow = dataFrameOf(
            "ObjectID" to listOf(objectId),
            "CameraID" to listOf(cameraId),
            "FocalLength" to listOf(focalLength),
            "PixelSize" to listOf(pixelSize),
            "SRS" to listOf(srs)
        ).cast<CamerasTable>()
        camerasTable = camerasTable.concat(newRow)
    }

    fun appendOrientedImagery(
        focalLength: Double,
        pixelSize: Double,
        srs: String,
        imagePath: String,
        x: Double,
        y: Double,
        z: Double,
        omega: Double,
        phi: Double,
        kappa: Double
    ) {
        val newRow = dataFrameOf(
            "FocalLength" to listOf(focalLength),
            "PixelSize" to listOf(pixelSize),
            "SRS" to listOf(srs),
            "ImagePath" to listOf(imagePath),
            "X" to listOf(x),
            "Y" to listOf(y),
            "Z" to listOf(z),
            "Omega" to listOf(omega),
            "Phi" to listOf(phi),
            "Kappa" to listOf(kappa)
        ).cast<OrientedImageryTable>()
        orientedImageryTable = orientedImageryTable.concat(newRow)
    }

    fun saveToCsv(directory: File) {
        val framesCsvFile = File(directory, "frames.csv")
        framesCsvFile.parentFile?.mkdirs() ?: throw IllegalStateException("Failed to create parent directory for CSV file: ${framesCsvFile.absolutePath}")
        if (framesCsvFile.exists()) {
            framesCsvFile.delete()
        }
        val camerasCsvFile = File(directory, "cameras.csv")
        camerasCsvFile.parentFile?.mkdirs() ?: throw IllegalStateException("Failed to create parent directory for CSV file: ${camerasCsvFile.absolutePath}")
        if (camerasCsvFile.exists()) {
            camerasCsvFile.delete()
        }
        val orientedImageryCsvFile = File(directory, "oriented_imagery.csv")
        orientedImageryCsvFile.parentFile?.mkdirs() ?: throw IllegalStateException("Failed to create parent directory for CSV file: ${orientedImageryCsvFile.absolutePath}")
        if (orientedImageryCsvFile.exists()) {
            orientedImageryCsvFile.delete()
        }
        framesTable.writeCsv(framesCsvFile)
        camerasTable.writeCsv(camerasCsvFile)
        orientedImageryTable.writeCsv(orientedImageryCsvFile)
    }
}
