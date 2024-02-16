/*
 * COPYRIGHT 1995-2024 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object AttachmentElementDefaults {
    
    val buttonBorderThickness = 2.dp
    val borderThickness = 1.dp
    val containerShape = RoundedCornerShape(5.dp)
    val attachmentDetailShape = RoundedCornerShape(10.dp)
    val attachmentShape = RoundedCornerShape(10.dp)
    
    @Composable
    fun colors() : AttachmentElementColors = AttachmentElementColors(
        containerColor = MaterialTheme.colorScheme.background,
        carouselContainerColor = MaterialTheme.colorScheme.onBackground,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

public data class AttachmentElementColors(
    val containerColor : Color,
    val carouselContainerColor: Color,
    val borderColor : Color,
)

