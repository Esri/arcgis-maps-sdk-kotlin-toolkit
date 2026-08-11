package com.arcgismaps.toolkit.arrealitycaptureapp.io

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame

const val CamerasTable_OBJECT_ID = 1
const val CamerasTable_CAMERA_ID = "cam_01"

@DataSchema
interface CamerasTable {
    val ObjectID: Int
    val CameraID: String
    val FocalLength: Double
    val PixelSize: Double
    val SRS: String
}

fun CamerasTable(): DataFrame<CamerasTable> = emptyDataFrame()
