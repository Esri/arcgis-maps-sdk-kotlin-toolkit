package com.arcgismaps.toolkit.geocompose

import android.view.View
import androidx.compose.runtime.Composable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.zero

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

public data class Callout(
    public val location: Point,
    public val offset: DoubleXY = DoubleXY.zero,
    public val rotateOffsetWithGeoView: Boolean = false,
    public val animated: Boolean = false,
    public var content: (@Composable () -> Unit)? = null,
) {

}
