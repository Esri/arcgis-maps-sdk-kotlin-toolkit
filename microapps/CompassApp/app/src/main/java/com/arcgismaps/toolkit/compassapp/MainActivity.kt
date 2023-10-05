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

package com.arcgismaps.toolkit.compassapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compassapp.screens.MapViewModel
import com.arcgismaps.toolkit.compassapp.screens.MapViewModelFactory
import com.arcgismaps.toolkit.compassapp.ui.theme.AppTheme
import com.arcgismaps.toolkit.composablemap.ComposableMap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set an API key
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        setContent {
            // define a theme
            AppTheme {
                // set your Composable content
                LocationApp()
            }
        }
    }
}

@Composable
fun LocationApp() {

    /////////
    // 1. Display some text
    /////////
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        // TODO maybe add an image
//        Text(text = "Welcome to Dev Summit 2023!")
//    }

    /////////
    // 2. display a map
    /////////
    // create an ArcGISMap with a basemap style
    val map = ArcGISMap(BasemapStyle.OsmStreets)
    // instantiate a MapViewModel using the factory
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    // show a composable map using the mapViewModel
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )

    // set the composable map's viewpoint to Germany
    mapViewModel.setViewpoint(Viewpoint(51.852, 10.477, 10e6))

    /////////
    //
}

@Preview(showSystemUi = true)
@Composable
fun LocationAppPreview() {
    AppTheme {
        LocationApp()
    }
}

// just map
//@Composable
//fun LocationApp() {
//    // create an ArcGISMap with a basemap style
//    val map = ArcGISMap(BasemapStyle.OsmStreets)
//    // instantiate a MapViewModel using the factory
//    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
//    // show a composable map using the mapViewModel
//    ComposableMap(
//        modifier = Modifier.fillMaxSize(),
//        mapInterface = mapViewModel
//    )
//    // set the composable map's viewpoint to Germany
//    mapViewModel.setViewpoint(Viewpoint(51.852, 10.477, 10e6))
//}
