package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.DrawStatus
import kotlinx.coroutines.flow.MutableStateFlow

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

public class EventForwarder {
    internal var drawStatus: MutableStateFlow<DrawStatus?> = MutableStateFlow(DrawStatus.InProgress)
}
