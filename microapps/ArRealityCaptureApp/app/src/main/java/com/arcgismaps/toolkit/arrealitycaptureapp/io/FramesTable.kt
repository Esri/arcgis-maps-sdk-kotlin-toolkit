package com.arcgismaps.toolkit.arrealitycaptureapp.io

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame

@DataSchema
interface FramesTable {
    val ObjectID: Int
    val Raster: String
    val CameraID: String
    val PerspectiveX: Double
    val PerspectiveY: Double
    val PerspectiveZ: Double
    val Omega: Double
    val Phi: Double
    val Kappa: Double
}

fun FramesTable(): DataFrame<FramesTable> = emptyDataFrame()
