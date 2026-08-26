package com.arcgismaps.toolkit.arrealitycaptureapp.io

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

/**
 * Data schema for the FramesTable as specified here:
 * https://doc.esri.com/en/arcgis-pro/latest/help/data/imagery/frames-table-schema.html
 */
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

