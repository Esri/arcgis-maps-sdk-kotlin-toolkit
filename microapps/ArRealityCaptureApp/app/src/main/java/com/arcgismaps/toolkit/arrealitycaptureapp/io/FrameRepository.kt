package com.arcgismaps.toolkit.arrealitycaptureapp.io

import android.os.Environment
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import java.io.File

private const val ANDROID_KOTLIN_DATA_DIR = "/Data/androidKotlin"

class FrameRepository {

    val androidKotlinRootDir by lazy {
        File(Environment.getExternalStorageDirectory(), ANDROID_KOTLIN_DATA_DIR)
    }

//    private val camerasTable = CamerasTable().apply {
//        this.append(
//            "ObjectID", 1,
//            "CameraID", "cam_01",
//            "FocalLength", 35.0,
//            "PixelSize", 0.005
//        )
//    }

    //private val framesTable = createFramesTable().apply {

//        this.append(
//            "ObjectID", 1,
//            "Raster", "frame_0001.jpg",
//            "CameraID", "cam_01",
//            "PerspectiveX", 10.0,
//            "PerspectiveY", 20.0,
//            "PerspectiveZ", 30.0,
//            "Omega", 0.1,
//            "Phi", 0.2,
//            "Kappa", 0.3,
//        )
    //}

    private val framesTable = dataFrameOf(
        "ObjectID" to columnOf(1),
            "Raster" to columnOf("frame_0001.jpg"),
            "CameraID" to columnOf("cam_01"),
            "PerspectiveX" to columnOf(10.0),
            "PerspectiveY" to columnOf(20.0),
            "PerspectiveZ" to columnOf(30.0),
            "Omega" to columnOf(0.1),
            "Phi" to columnOf(0.2),
            "Kappa" to columnOf(0.3)
    ).cast<FramesTable>()

    fun saveToCsv() {
        framesTable.writeCsv(File(androidKotlinRootDir,"realitycapture/frames.csv"))
    }
}
