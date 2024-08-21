package com.arcgismaps.toolkit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


internal object ExpandableCardDefaults {
    @Composable
    fun shapes(): ExpandableCardShapes = ExpandableCardShapes(
        padding = 16.dp,
        containerShape = RoundedCornerShape(5.dp),
        borderThickness = 1.dp
    )
    @Composable
    fun colors() : ExpandableCardColors = ExpandableCardColors(
        headerTextColor = MaterialTheme.colorScheme.onBackground,
        containerColor = MaterialTheme.colorScheme.background,
        galleryContainerColor = MaterialTheme.colorScheme.onBackground,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

internal data class ExpandableCardShapes(
    val padding: Dp,
    val containerShape: RoundedCornerShape,
    val borderThickness: Dp
)

internal data class ExpandableCardColors(
    val headerTextColor: Color,
    val containerColor : Color,
    val galleryContainerColor: Color,
    val borderColor : Color,
)


