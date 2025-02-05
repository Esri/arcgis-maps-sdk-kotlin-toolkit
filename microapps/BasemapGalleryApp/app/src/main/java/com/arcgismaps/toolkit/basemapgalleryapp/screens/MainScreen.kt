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

package com.arcgismaps.toolkit.basemapgalleryapp.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.toolkit.basemapgallery.BasemapGallery
import com.arcgismaps.toolkit.basemapgalleryapp.ViewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView

/**
 * The main screen of the application consisting of a [MapView] and a [BasemapGallery]. Clicking on
 * an item in the gallery will set that basemap in the map view.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen() {
    val viewModel: ViewModel = viewModel()

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        sheetPeekHeight = 128.dp,
        sheetContent = {
            BasemapGallery(modifier = Modifier.fillMaxHeight(fraction = 0.5f),
                basemapGalleryItems = viewModel.items,
                onItemClick = {
                    when (val tag = it.tag) {
                        is BasemapStyleInfo -> {
                            Log.d("BasemapGallery", "Item clicked: ${tag.styleName}")
                            viewModel.arcGISMap.setBasemap(Basemap(tag.style))
                        }

                        else -> Log.d("BaseMapGalley", "Item clicked: tag type is not handled")
                    }
                })
        },
        scaffoldState = bottomSheetScaffoldState,
        topBar = {
            TopAppBar(title = { Text("Basemap Gallery App") })
        },
    ) { paddingValues ->
        MapView(
            modifier = Modifier.padding(paddingValues),
            arcGISMap = viewModel.arcGISMap)
    }
}
