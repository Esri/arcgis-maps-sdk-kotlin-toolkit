package com.arcgismaps.toolkit.arrealitycaptureapp.io

import android.content.Context
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FrameRepository(private val context: Context) {

    private var framesTable = emptyDataFrame<FramesTable>()
    private var camerasTable = emptyDataFrame<CamerasTable>()

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
        framesTable.writeCsv(framesCsvFile)
        camerasTable.writeCsv(camerasCsvFile)
    }
}
