package com.arcgismaps.toolkit.scalebar.internal

/**
 * Represents a Scalebar label.
 *
 * @param index The index of the label.
 * @param xOffset The x offset of the label.
 * @param yOffset The y offset of the label.
 * @param text The text of the label.
 *
 * @since 200.7.0
 */
internal data class ScalebarLabel(
    val index: Int,
    val xOffset: Double,
    val yOffset: Double,
    val text: String
)
