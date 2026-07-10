package com.arcgismaps.toolkit.arrealitycaptureapp.io

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame

@DataSchema
interface CamerasTable {
    val ObjectID: Int
    val CameraID: String
    val FocalLength: Double
    val PixelSize: Double
}

fun CamerasTable(): DataFrame<CamerasTable> = emptyDataFrame()
