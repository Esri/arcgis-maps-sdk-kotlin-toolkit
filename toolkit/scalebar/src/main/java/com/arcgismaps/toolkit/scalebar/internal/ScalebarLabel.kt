package com.arcgismaps.toolkit.scalebar.internal

internal data class ScalebarLabel(
    val index: Int,
    val xOffset: Float,
    val text: String
) {
    companion object {
//        val yOffset: Float
//            get() = Scalebar.fontHeight / 2.0f
    }
}