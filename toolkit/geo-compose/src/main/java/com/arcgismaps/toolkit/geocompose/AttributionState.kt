/*
 * COPYRIGHT 1995-2023 ESRI
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

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent

/**
 * Data class for attribution bar related properties/events on the [com.arcgismaps.toolkit.geocompose.MapView].
 *
 * @since 200.3.0
 */
@Stable
public data class AttributionState(
    val isAttributionBarVisible: Boolean = true,
    val onAttributionTextChanged: ((String) -> Unit)? = null,
    val onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null
)

