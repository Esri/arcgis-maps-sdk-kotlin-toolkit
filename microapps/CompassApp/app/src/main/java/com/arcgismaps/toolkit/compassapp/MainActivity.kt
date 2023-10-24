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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.indoors.FloorFilter
import com.arcgismaps.toolkit.indoors.FloorFilterState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAPIKey()

        setContent {
                TextDemo()
        }
    }
}

@Composable
fun TextDemo() {

    Text(text = "Welcome to European Dev Summit 2023!",
        fontSize = 20.sp, modifier = Modifier.padding(20.dp, 200.dp, 0.dp, 0.dp))

    Image(painter = painterResource(id = R.drawable.kotlin_hero),
        contentDescription = null, modifier = Modifier.padding(0.dp, 200.dp, 0.dp, 0.dp))
}







@Composable
fun MapDemo() {
    // create an arcGISMap with basemap style and initial viewpoint
    val arcGISMap = remember { ArcGISMap(BasemapStyle.OsmStreets).apply {
        initialViewpoint = Viewpoint(51.852, 10.477, 10e6)
    } };
    // instantiate map state and assign it the ArcGISMap
    // TODO: the MapViewModel will be replaced by new MapState API
    val mapState = viewModel<MapViewModel>(factory = MapViewModelFactory(arcGISMap))

    // create a composable Map and assign the mapstate to it
    // TODO: ComposableMap will become MapView
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapState
    )
    // TODO: this line will be removed once viewpoint bug is fixed
    mapState.setViewpoint(Viewpoint(51.852, 10.477, 10e6))
}



@Composable
fun LocationDemo() {
    val scope = rememberCoroutineScope()
    val map by produceState<ArcGISMap?>(initialValue = null) {
        val mmpk = MobileMapPackage(
            "/data/user/0/com.arcgismaps.toolkit.compassapp/files/Berlin_Kotlin_23.mmpk"
        )
        mmpk.load().getOrNull() ?: return@produceState
        if (mmpk.maps.isEmpty()) return@produceState
        value = mmpk.maps[0]
    }
    val floorFilterState = map?.let { remember { FloorFilterState(it, scope) } }

    // TODO: create a location display once API is ready
    // get the location data source from the location display
    val locationDataSource = remember { SystemLocationDataSource() }

    val mapState = map?.let {
        viewModel<MapViewModel>(factory = MapViewModelFactory(map, locationDataSource = locationDataSource))
    }

    // collect location updates
    val lastKnownLocation = locationDataSource.locationChanged.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        // start the location data source
        locationDataSource.start()
            .onFailure { Log.i("LocationDemo", "Failed to start location data source") }
    }

    Scaffold(
        floatingActionButton = {
            // display a button that zooms to the current location
            FloatingActionButton(
                onClick = {
                    lastKnownLocation.value?.let {
                        // TODO: the inner safe (?.) call can be removed eventually
                        mapState?.setViewpoint(Viewpoint(it.position, 2000.0))
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

        if (floorFilterState != null && mapState != null) {

            // show a composable map using the mapViewModel
            ComposableMap(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                mapInterface = mapState
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp, 40.dp), // TODO: needed? named params?
                    contentAlignment = Alignment.BottomStart
                ) {
                    // TODO: initial floor as 1. Also bug fixes to display levels.
                    FloorFilter(floorFilterState = floorFilterState)
                    // TODO: probably shouldn't set viewpoint here, use initialViewpoint once that is working
                    //  or else move this to a different place
                    mapState.setViewpoint(Viewpoint(52.5119, 13.3922, 100000.0))
                }
            }
        }
    }
}




















private val Color.Companion.Purple get() = Color(185, 27, 219)

private fun MainActivity.setupAPIKey() {
    // set an API key
    ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
    LicenseKey.create(BuildConfig.LICENSE_KEY)
        ?.let { ArcGISEnvironment.setLicense(it) }
        ?: throw IllegalStateException("Invalid license key set in onCreate")
    ArcGISEnvironment.applicationContext = this
}
