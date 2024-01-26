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

package com.arcgismaps.toolkit.mapviewidentifyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.arcgismaps.toolkit.mapviewidentifyapp.ui.theme.MapViewIdentifyAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)

        val appState = AppState()

        lifecycleScope.launch {
            appState.map.value = ArcGISMap(
                "https://www.arcgis.com/home/item.html?id=a80b634189d9430980792d2df323a2be"
            )
        }

        setContent {
            MapViewIdentifyAppTheme {
                MainScreen(appState)
            }
        }
    }
}

private class AppState(
    val map: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
)

@Composable
private fun MainScreen(appState: AppState) {
    val map by appState.map.collectAsState()
    var totalRotation by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1.0f) }

    val scope = rememberCoroutineScope()
    val proxy = remember { MapViewProxy() }
    var myScreenPoint by remember { mutableStateOf("None") }

    var numIdentified by remember { mutableStateOf(0) }
    var mapViewHeight by remember { mutableStateOf(0) }
    var mapViewWidth by remember { mutableStateOf(0) }

    val predefinedViewpoint =
        remember {
//            MapViewpointOperation.Animate(
//                Viewpoint(
//                    Point(
//                        50_000.0,
//                        50_000.0
//                    ),
//                    300_000.0
//                ),
//                6.seconds
//            )
            MapViewpointOperation.Set(
                Viewpoint(
                    Point(
                        50_000.0,
                        50_000.0
                    ),
                    300_000.0
                )
            )
        }

    var currentViewpointOperation by remember {
        mutableStateOf<MapViewpointOperation?>(null, neverEqualPolicy())
    }

    Column {
        Text(myScreenPoint)
        Text("identified: $numIdentified")
        Button(onClick = {
            scope.launch {
                val result = proxy.identifyLayers(ScreenCoordinate(mapViewWidth / 2.0, mapViewHeight / 2.0), 10.dp)
                numIdentified = result.getOrNull()?.size ?: 0
            }
        }) { Text("Update identified count") }
        Button(onClick = {
            currentViewpointOperation = predefinedViewpoint
        }) {
            Text("Go to predefined viewpoint")
        }
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .onGloballyPositioned {
                    mapViewWidth = it.size.width
                    mapViewHeight = it.size.height
                },
            arcGISMap = map,
            mapViewProxy = proxy,
            viewpointOperation = currentViewpointOperation,
            onRotate = {
                when (it.status) {
                    RotationChangeEvent.RotationStatus.Start, RotationChangeEvent.RotationStatus.End -> totalRotation =
                        0f

                    RotationChangeEvent.RotationStatus.Rotating -> {
                        totalRotation += it.deltaSinceLastEvent.toFloat()
                        val a = (totalRotation % 360) / 360f
                        alpha = 1 - if (a < 0) -1 * a else a
                    }
                }
            },
            onUp = { e ->
                myScreenPoint = proxy.screenToLocationOrNull(e.screenCoordinate).toString()
            }
        )
    }
}
