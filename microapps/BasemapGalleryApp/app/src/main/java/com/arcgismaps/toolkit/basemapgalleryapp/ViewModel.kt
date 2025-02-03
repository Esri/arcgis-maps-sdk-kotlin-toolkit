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

package com.arcgismaps.toolkit.basemapgalleryapp

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

/**
 * The view model.
 *
 * @param application the applicatinon associated with this view model
 * @constructor constructs a viewmodel
 */
class ViewModel(application: Application) : AndroidViewModel(application) {

    var items = mutableStateListOf<BasemapGalleryItem>()
        private set

    val mapViewProxy = MapViewProxy()
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery)

    init {
        viewModelScope.launch {
            // get basemaps from a basemap style service
            val service = BasemapStylesService()
            service.load().getOrThrow()

            // for each basemap style info create a gallery item and add it to the list of items
            service.info?.stylesInfo?.forEach { basemapStyleInfo ->
                val galleryItem = BasemapGalleryItem(basemapStyleInfo)
                items.add(galleryItem)
            }
        }
    }
}
