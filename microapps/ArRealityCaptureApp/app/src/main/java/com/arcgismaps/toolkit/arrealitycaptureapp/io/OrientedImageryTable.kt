package com.arcgismaps.toolkit.arrealitycaptureapp.io

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

/**
 * Data schema for the OrientedImageryTable as specified here:
 * https://doc.esri.com/en/arcgis-pro/latest/help/data/imagery/oriented-imagery-table.html
 */
@DataSchema
interface OrientedImageryTable {
    val FocalLength: Double
    val PixelSize: Double
    val SRS: String
    val ImagePath: String
    val X: Double
    val Y: Double
    val Z: Double
    val Omega: Double
    val Phi: Double
    val Kappa: Double
}
