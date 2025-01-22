package com.arcgismaps.toolkit.scalebar.internal

/**
 * Represents a Scalebar division.
 *
 * @param index The index of the division.
 * @param xOffset The x offset of the division.
 * @param labelYOffset The y offset of the division's label from the scalebar.
 * @param label The text of the division's label.
 *
 * @since 200.7.0
 */
internal data class ScalebarDivision(
    val index: Int,
    val xOffset: Double,
    val labelYOffset: Double,
    val label: String
)
