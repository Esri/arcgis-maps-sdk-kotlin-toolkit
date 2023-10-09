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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.License
import com.arcgismaps.LicenseKey
import com.arcgismaps.location.SystemLocationDataSource
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
        LicenseKey.create(BuildConfig.LICENSE_KEY)
            ?.let { ArcGISEnvironment.setLicense(it) }
            ?: throw IllegalStateException("Invalid license key set in onCreate")
        ArcGISEnvironment.applicationContext = this
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
//        verticalArrangement = Arrangement.Center,
//        modifier = Modifier.fillMaxSize()
//    ) {
//        // TODO: adjust text size based on the size of your device screen!! (emulator or physical)
//        Text(text = "Welcome to Dev Summit 2023!", fontSize = 30.sp, textAlign = TextAlign.Center)
//        Image(painter = painterResource(id = R.drawable.kotlin_hero), contentDescription = null)
//    }

    /////////
    // 2. display a map
    /////////
    // create an ArcGISMap with a basemap style, and a location data source for displaying the current location
    val map = remember { ArcGISMap(BasemapStyle.OsmStreets) }
    val locationDataSource = remember { SystemLocationDataSource() }

    // instantiate a MapViewModel using the factory
    val mapViewModel =
        viewModel<MapViewModel>(factory = MapViewModelFactory(map, locationDataSource = locationDataSource))

    val lastLocation = locationDataSource.locationChanged.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        // set the composable map's viewpoint to Germany
        mapViewModel.setViewpoint(Viewpoint(51.852, 10.477, 10e6))

        // start the location data source
        locationDataSource.start()
            .onFailure { Log.i("LocationApp", "Failed to start location data source") }
    }

    Scaffold(
        floatingActionButton = {
            // display a button that zooms to the current location
            FloatingActionButton(
                onClick = {
                    lastLocation.value?.let {
                        mapViewModel.setViewpoint(Viewpoint(it.position, 10e3))
                    }
                },
                modifier = Modifier.padding(40.dp).size(80.dp),
                shape = CircleShape,
                containerColor = Color.Purple
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location),
                    contentDescription = "Go to current location",
                    modifier = Modifier.fillMaxSize(0.75f),
                    tint = Color.White
                )
            }
        }
    ) {

        // show a composable map using the mapViewModel
        ComposableMap(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            mapInterface = mapViewModel
        )
    }



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

private val Color.Companion.Purple get() = Color(185, 27, 219)
