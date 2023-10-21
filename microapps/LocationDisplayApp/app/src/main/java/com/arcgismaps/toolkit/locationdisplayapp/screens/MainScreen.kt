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

package com.arcgismaps.toolkit.locationdisplayapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.rememberLocationDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Displays a [Map] with a [LocationDisplay].
 * The location display can be started/stopped using a [Switch].
 */
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
    val checked = remember { mutableStateOf(false) }
    val locationDisplay = rememberLocationDisplay {
        start(scope) {
            checked.value = it.isSuccess
        }
    }

    Column {
        Row(modifier = Modifier.padding(5.dp)) {
            Switch(
                checked.value,
                onCheckedChange = {
                    checked.value = it
                    if (locationDisplay.isStarted) {
                        locationDisplay.stop(scope)
                        checked.value = false
                    } else {
                        locationDisplay.start(scope) { result ->
                            checked.value = result.isSuccess
                        }
                    }
                }
            )
        }
        Map(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
            locationDisplay = locationDisplay
        )
    }
}

/**
 * Extension property on [LocationDisplay] to check its started status.
 */
private val LocationDisplay.isStarted: Boolean
    get() = dataSource.status.value == LocationDataSourceStatus.Started

/**
 * Extension function on [LocationDisplay] to start [this].
 * [onCompletion] is called when starting has completed.
 */
private fun LocationDisplay.start(
    scope: CoroutineScope,
    onCompletion: ((Result<Unit>) -> Unit)? = null
) {
    scope.launch {
        dataSource.start().let { result ->
            onCompletion?.let {
                it.invoke(result)
            }
        }
    }
}

/**
 * Extension function on [LocationDisplay] to stop [this].
 */
private fun LocationDisplay.stop(scope: CoroutineScope) {
    scope.launch {
        dataSource.stop()
    }
}
