/*
 *
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

package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.compassapp.navigation.CompassScreens
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.DuplexFlow

@Composable
fun MainScreen(navController: NavController) {
//fun MainScreen() {
    // create an ArcGISMap with a Topographic basemap style
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    // instantiate a MapViewModel using the factory
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    // hoist the mapRotation state
    val mapRotation by mapViewModel.mapRotation.collectAsState(DuplexFlow.Type.Read)
    // show a composable map using the mapViewModel
    val modifier = Modifier.fillMaxSize()
    val modifierstate = remember { mutableStateOf(modifier) }

    Column {
        Button(onClick = {
            //your onclick code here
            navController.navigate(route = CompassScreens.SecondaryScreen.name)
//            modifierstate.value = Modifier.fillMaxSize().padding(horizontal = 60.dp, vertical = 40.dp)
        }) {
            Text(text = "Switch MapView")
        }

        ComposableMap(
//            modifier = Modifier.fillMaxSize()
            modifier = modifierstate.value
//                .padding(horizontal = 40.dp, vertical = 40.dp)
            ,
            mapInterface = mapViewModel
        ) {
//            Button(onClick = {
//                //your onclick code here
//                navController.navigate(route = CompassScreens.SecondaryScreen.name)
//            }) {
//                Text(text = "Switch MapView")
//            }
            Row(modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
                .padding(25.dp)) {
                // show the compass and pass the mapRotation state data
                Compass(rotation = mapRotation) {
                    // reset the ComposableMap viewpoint rotation to point north using the mapViewModel
                    mapViewModel.setViewpointRotation(0.0)
                }
            }
        }
        // set the composable map's viewpoint to North America
        mapViewModel.setViewpoint(Viewpoint(39.8, -98.6, 10e7))
    }


}
