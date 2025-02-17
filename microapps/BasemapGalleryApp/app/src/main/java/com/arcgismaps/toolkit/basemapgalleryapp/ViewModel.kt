/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.basemapgalleryapp

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

/**
 * The view model.
 *
 * @param application the application associated with this view model
 * @constructor constructs a viewmodel
 *
 * @since 200.7.0
 */
class ViewModel(application: Application) : AndroidViewModel(application) {

    val proxy = MapViewProxy()

    var styleItems = mutableStateListOf<BasemapGalleryItem>()
        private set
    var portalItems = mutableStateListOf<BasemapGalleryItem>()
        private set

    private val initialViewpoint = Viewpoint(
        center = Point(-11e6, 5e6, SpatialReference.webMercator()),
        scale = 1e8
    )

    var arcGISMap =
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint =
                this@ViewModel.initialViewpoint
        }

    init {
        viewModelScope.launch {
            // get basemap portal items from a portal
            val portal = Portal("https://www.arcgis.com")
            portal.load()
                .onFailure { Log.w("BasemapGallery", "Failed to load ${portal.url}") }
                .onSuccess {
                    portal.fetchBasemaps()
                        .onFailure { Log.w("BasemapGallery", "Failed to fetch basemaps") }
                        .onSuccess {
                            it.forEach { basemap ->
                                basemap.item?.let { item ->
                                    val galleryItem = BasemapGalleryItem(item)
                                    portalItems.add(galleryItem)
                                }
                            }
                        }
                }

            // get basemap style info from a basemap style service
            val service = BasemapStylesService()
            service.load()
                .onFailure { Log.w("BasemapGallery", "Failed to load basemap styles") }
                .onSuccess {
                    // for each basemap style info create a gallery item and add it to the list of items
                    service.info?.stylesInfo?.forEach { basemapStyleInfo ->
                        val galleryItem = BasemapGalleryItem(basemapStyleInfo)
                        styleItems.add(galleryItem)
                    }
                }
        }
    }

    /**
     * Change the basemap and reset the viewpoint.
     *
     * @param basemap to set
     * @since 200.7.0
     */
    fun changeBasemap(basemap: Basemap) {
        proxy.setViewpoint(initialViewpoint)
        arcGISMap.setBasemap(basemap)
    }
}
