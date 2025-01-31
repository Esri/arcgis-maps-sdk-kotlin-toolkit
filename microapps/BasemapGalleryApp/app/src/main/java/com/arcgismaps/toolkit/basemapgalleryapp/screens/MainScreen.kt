/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.basemapgalleryapp.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.mapping.Item
import com.arcgismaps.toolkit.basemapgallery.BasemapGallery
import com.arcgismaps.toolkit.basemapgalleryapp.ViewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun MainScreen() {
    val viewModel: ViewModel = viewModel()

    Column {
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5f),
            arcGISMap = viewModel.arcGISMap,
            mapViewProxy = viewModel.mapViewProxy
        )
        BasemapGallery(modifier = Modifier.weight(0.5f), basemapGalleryItems = viewModel.items, onItemClick = {
            when (val tag = it.tag) {
                is BasemapStyleInfo -> {
                    Log.d("BasemapGallery", "Item clicked: ${tag.styleName}")
                    viewModel.arcGISMap.setBasemap(Basemap(tag.style))
                }
                is Item -> {
                    Log.d("BasemapGallery", "Item clicked: ${tag.itemId}")
                    viewModel.arcGISMap.setBasemap(Basemap(tag))
                }
                else -> Log.d("BaseMapGalley", "Item clicked: tag type is not handled")
            }
        })
    }
}
