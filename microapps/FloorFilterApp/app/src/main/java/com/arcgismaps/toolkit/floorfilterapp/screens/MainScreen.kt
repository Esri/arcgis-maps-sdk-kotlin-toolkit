/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.floorfilterapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.indoors.FloorFilter

@Composable
fun MainScreen() {
    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "b4b599a43a474d33946cf0df526426f5")
    val floorAwareWebMap = ArcGISMap(portalItem)

    val map by produceState<ArcGISMap?>(initialValue = null) {
        // this data has no sites, and level.shortName is blank
        val mmpk = MobileMapPackage(
            "/data/user/0/com.arcgismaps.toolkit.floorfilterapp/files/Berlin_Kotlin_23.mmpk"
        )
        // this data has no sites
//        val mmpk = MobileMapPackage(
//            "/data/user/0/com.arcgismaps.toolkit.floorfilterapp/files/sf_mixedlayers_overlay1.mmpk"
//        )
        mmpk.load().getOrNull() ?: return@produceState
        if (mmpk.maps.isEmpty()) return@produceState
        value = mmpk.maps[0]
    }

//    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(floorAwareWebMap))
    val mapViewModel = map?.let { viewModel<MapViewModel>(factory = MapViewModelFactory(it)) }

    if (mapViewModel != null) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            mapInterface = mapViewModel
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                FloorFilter(floorFilterState = mapViewModel.floorFilterState)
            }
        }
    }
}
