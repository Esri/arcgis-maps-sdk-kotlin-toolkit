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

package com.arcgismaps.toolkit.mapcomposeapp.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.WrapAroundMode
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.MapProperties
import com.arcgismaps.toolkit.geocompose.MapState
import kotlin.math.roundToInt

@Composable
fun MainScreen() {
    val mapProperties = remember {
        MapProperties(arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreetsNight))
    }

    val mapState = MapState(rememberCoroutineScope(), mapProperties)


    LaunchedEffect(Unit) {
        mapState.drawStatus.collect {
            Log.e("DrawStatus", "DrawStatus: ${it?.toString()}")
        }
    }

    LaunchedEffect(Unit) {
        mapState.onSingleTapConfirmed.collect {
            Log.e("MapTapped", "MapTapped: ${it?.mapPoint?.x},${it?.mapPoint?.y}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Map(
            modifier = Modifier.size(500.dp), // Not able to use Modifier.weight here, not entirely sure why.
            mapState = mapState,
        )
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Toggle wrap around mode")

            val wrapAroundMode = mapProperties.wrapAroundMode.collectAsState()
            val isChecked = wrapAroundMode.value is WrapAroundMode.EnabledWhenSupported

            Switch(
                checked = isChecked,
                onCheckedChange = {
                    if (it) {
                        mapProperties.wrapAroundMode.value = WrapAroundMode.EnabledWhenSupported
                    } else {
                        mapProperties.wrapAroundMode.value = WrapAroundMode.Disabled
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Toggle ArcGIS map day/night")

            val arcGISMap = mapProperties.arcGISMap.collectAsState().value
            var isMapLoaded by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                arcGISMap?.load()?.onSuccess {
                    isMapLoaded = true
                }
            }

            val isNightMode = arcGISMap?.basemap?.collectAsState()?.value?.name.toString()
                .contains("StreetsNight")
            Switch(
                enabled = isMapLoaded,
                checked = isNightMode,
                onCheckedChange = {
                    if (it) {
                        mapProperties.arcGISMap.value = ArcGISMap(BasemapStyle.ArcGISStreetsNight)
                    } else {
                        mapProperties.arcGISMap.value = ArcGISMap(BasemapStyle.ArcGISStreets)
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var rotation by rememberSaveable {
                mutableStateOf(0.0)
            }
            LaunchedEffect(Unit){
                mapState.mapRotation.collect{
                    rotation = it
                }
            }
            Text(text = "Current map rotation degrees: ${rotation.roundToInt()}")
        }
    }
}
