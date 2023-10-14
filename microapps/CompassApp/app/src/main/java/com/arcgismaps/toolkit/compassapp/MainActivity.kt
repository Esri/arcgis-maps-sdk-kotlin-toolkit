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

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LicenseKey
import com.arcgismaps.location.SystemLocationDataSource
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compassapp.screens.MapViewModel
import com.arcgismaps.toolkit.compassapp.screens.MapViewModelFactory
import com.arcgismaps.toolkit.compassapp.ui.theme.AppTheme
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.indoors.FloorFilter
import com.arcgismaps.toolkit.indoors.FloorFilterState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAPIKey(this);
        Log.i("xcv", filesDir.toString())

        setContent {
                TextDemo()
//                MapDemo()
//                LocationDemo()
        }
    }
}

@Composable
fun TextDemo() {
    Text(text = "Welcome to European Dev Summit 2023!", fontSize = 20.sp, modifier = Modifier.padding(20.dp, 200.dp, 0.dp, 0.dp))
    Image(painter = painterResource(id = R.drawable.kotlin_hero), contentDescription = null, modifier = Modifier.padding(0.dp, 200.dp, 0.dp, 0.dp))
}

@Composable
fun MapDemo() {
    // create an ArcGISMap with a basemap style
    val map = remember { ArcGISMap(BasemapStyle.OsmStreets) }
    // instantiate a MapViewModel using the factory
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    // show a composable map using the mapViewModel
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )
    // set the composable map's viewpoint to Germany
    mapViewModel.setViewpoint(Viewpoint(51.852, 10.477, 10e6))
}

@Composable
fun LocationDemo() {
    val map by produceState<ArcGISMap?>(initialValue = null) {
        val mmpk =
            MobileMapPackage("/data/user/0/com.arcgismaps.toolkit.compassapp/files/BerlinDevSummit_10_20.mmpk")
        mmpk.load().onSuccess { Log.i("xcv", "loaded mmpk with ${mmpk.maps.size} maps") }.getOrThrow()
//        mmpk.load().getOrNull() ?: return@produceState
//        if (mmpk.maps.isEmpty()) return@produceState
        value = mmpk.maps[0]
        // FIXME:
        //value = ArcGISMap("https://www.arcgis.com/home/item.html?id=f133a698536f44c8884ad81f80b6cfc7")
    }

    val locationDataSource = remember { SystemLocationDataSource() }

    val mapViewModel = map?.let {
        viewModel<MapViewModel>(factory = MapViewModelFactory(map, locationDataSource = locationDataSource))
    }

    val lastLocation = locationDataSource.locationChanged.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        // start the location data source
        locationDataSource.start()
            .onFailure { Log.i("LocationApp", "Failed to start location data source") }
    }

    val scope = rememberCoroutineScope()
    val floorFilterState = map?.let { remember { FloorFilterState(it, scope) } }

    Scaffold(
        floatingActionButton = {
            // display a button that zooms to the current location
            FloatingActionButton(
                onClick = {
                    lastLocation.value?.let {
                        // TODO: the inner safe (?.) call can be removed eventually
                        mapViewModel?.setViewpoint(Viewpoint(it.position, 2500.0))
                    }
                },
                modifier = Modifier
                    .padding(40.dp)
                    .size(80.dp),
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

        if (floorFilterState != null && mapViewModel != null) {

            // show a composable map using the mapViewModel
            ComposableMap(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                mapInterface = mapViewModel
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp, 40.dp), // TODO: needed? named params?
                ) {
                    // TODO: initial floor as 1. Also bug fixes to display levels.
                    FloorFilter(floorFilterState = floorFilterState)
                    mapViewModel.setViewpoint(Viewpoint(52.5119, 13.3922, 100000.0))
                }
            }
        }
    }

    //    // create an ArcGISMap with a basemap style, and a location data source for displaying the current location
//    val map = remember { ArcGISMap(BasemapStyle.OsmStreets) } // TODO: needs to use the MMPK?
//    val locationDataSource = remember { SystemLocationDataSource() }
//
//    // instantiate a MapViewModel using the factory
//    val mapViewModel =
//        viewModel<MapViewModel>(factory = MapViewModelFactory(map, locationDataSource = locationDataSource))
//
//    val lastLocation = locationDataSource.locationChanged.collectAsState(initial = null)
//
//    LaunchedEffect(Unit) {
//        // set the composable map's viewpoint to Germany
//        mapViewModel.setViewpoint(Viewpoint(51.852, 10.477, 10e6))
//
//        // start the location data source
//        locationDataSource.start()
//            .onFailure { Log.i("LocationApp", "Failed to start location data source") }
//    }
//
//    Scaffold(
//        floatingActionButton = {
//            // display a button that zooms to the current location
//            FloatingActionButton(
//                onClick = {
//                    lastLocation.value?.let {
//                        mapViewModel.setViewpoint(Viewpoint(it.position, 10e3))
//                    }
//                },
//                modifier = Modifier.padding(40.dp).size(80.dp),
//                shape = CircleShape,
//                containerColor = Color.Purple
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.my_location),
//                    contentDescription = "Go to current location",
//                    modifier = Modifier.fillMaxSize(0.75f),
//                    tint = Color.White
//                )
//            }
//        }
//    ) {
//
//        // show a composable map using the mapViewModel
//        ComposableMap(
//            modifier = Modifier
//                .padding(it)
//                .fillMaxSize(),
//            mapInterface = mapViewModel
//        )
//    }
}


















//@Preview(showSystemUi = true)
//@Composable
//fun AppPreview() {
//    AppTheme {
//        DemoApp()
//    }
//}

private val Color.Companion.Purple get() = Color(185, 27, 219)

private fun setupAPIKey(context: Context) {
    // set an API key
    ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
    LicenseKey.create(BuildConfig.LICENSE_KEY)
        ?.let { ArcGISEnvironment.setLicense(it) }
        ?: throw IllegalStateException("Invalid license key set in onCreate")
    ArcGISEnvironment.applicationContext = context
}
