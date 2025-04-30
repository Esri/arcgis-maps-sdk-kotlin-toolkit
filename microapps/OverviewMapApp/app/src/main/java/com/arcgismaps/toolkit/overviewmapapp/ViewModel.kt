package com.arcgismaps.toolkit.overviewmapapp

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.Viewpoint

/*
 COPYRIGHT 1995-2025 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

/**
 * The view model.
 *
 * @param application the application associated with this view model
 * @constructor constructs a viewmodel
 *
 * @since 200.8.0
 */
class ViewModel(application: Application) : AndroidViewModel(application) {
    private val initialViewpoint = Viewpoint(
        latitude = 39.8,
        longitude = -98.6,
        scale = 10e7
    )

    var viewpointForMapView = mutableStateOf(
        initialViewpoint
    )

    var viewpointForSceneView = mutableStateOf(
        initialViewpoint
    )
}