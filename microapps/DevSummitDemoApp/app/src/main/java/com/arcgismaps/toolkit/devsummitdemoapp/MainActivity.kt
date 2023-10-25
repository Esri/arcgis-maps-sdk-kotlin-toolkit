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

package com.arcgismaps.toolkit.devsummitdemoapp

import android.os.Bundle
import android.util.Log
import android.view.View
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
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LicenseKey
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.rememberLocationDisplay
import com.arcgismaps.toolkit.indoors.FloorFilter
import com.arcgismaps.toolkit.indoors.FloorFilterState
import com.arcgismaps.toolkit.indoors.UIProperties

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAPIKey()

        setContent {
            LocationDemo()
        }
    }
}

@Composable
fun TextDemo() {

    Text(
        text = "Welcome to European Dev Summit 2023!",
        fontSize = 20.sp, modifier = Modifier.padding(20.dp, 200.dp, 0.dp, 0.dp)
    )

    Image(
        painter = painterResource(id = R.drawable.kotlin_hero),
        contentDescription = null, modifier = Modifier.padding(0.dp, 200.dp, 0.dp, 0.dp)
    )
}


@Composable
fun MapDemo() {
    // create an arcGISMap with basemap style
    val arcGISMap = remember { ArcGISMap(BasemapStyle.OsmStreets) }

    // invoke the Map Composable, pass the ArcGISMap to it, and set a viewpoint
    Map(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = arcGISMap,
        viewpoint = Viewpoint(51.852, 10.477, 10e6),
    )
}

@Composable
fun LocationDemo() {
    val scope = rememberCoroutineScope()
    val map by rememberHiltonMap()

    var viewpoint by remember { mutableStateOf(Viewpoint(52.5119, 13.3922, 100000.0), neverEqualPolicy()) }

    // TODO(JEN): Comment on whether we should put this code into a helper function to hide it.
    val floorFilterState = map?.let {
        remember {
            FloorFilterState(it, scope, createUiProperties()).apply {
                selectedFacilityId = "Hilton Berlin"
            }
        }
    }

    val locationDisplay = rememberLocationDisplay()

    val lastKnownLocation = locationDisplay.location.collectAsState()

    LaunchedEffect(Unit) {
        locationDisplay.dataSource.start()
            .onFailure { Log.i("LocationDemo", "Failed to start location data source") }
        // TODO(JEN): Comment on whether we should keep this logging in the final demo.
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    lastKnownLocation.value?.let {
                        viewpoint = Viewpoint(it.position, 2000.0)
                    }
                },
                modifier = Modifier
                    .padding(40.dp)
                    .size(80.dp),
                shape = CircleShape,
                containerColor = Color.Purple,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location),
                    contentDescription = "Go to current location",
                    modifier = Modifier.fillMaxSize(0.75f),
                    tint = Color.White,
                )
            }
        }
    ) {

        Map(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            arcGISMap = map,
            locationDisplay = locationDisplay,
            viewpoint = viewpoint,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp, 40.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (floorFilterState != null) {
                    FloorFilter(floorFilterState = floorFilterState)
                }
            }
        }
    }
}
































@Composable
fun rememberHiltonMap(): State<ArcGISMap?> {
    val context = LocalContext.current
    return produceState<ArcGISMap?>(initialValue = null) {
        val mmpk = MobileMapPackage("${context.filesDir}/Berlin_Kotlin_23.mmpk")
        mmpk.load().getOrNull() ?: return@produceState
        if (mmpk.maps.isEmpty()) return@produceState
        val map = mmpk.maps[0]
        // Load these eagerly so that when we come to make the floor filter it won't need to
        // do the loading itself (which messes up our initialization of the floor filter).
        map.load().getOrNull() ?: return@produceState
        map.floorManager?.load()?.getOrNull() ?: return@produceState
        value = map
    }
}

fun createUiProperties(): UIProperties {
    return UIProperties(
        siteFacilityButtonVisibility = View.INVISIBLE,
        closeButtonVisibility = View.INVISIBLE,
        buttonSize = Size(75.dp.value, 55.dp.value),
        typography = Typography(labelLarge = TextStyle(fontSize = 20.sp)),
    )
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
