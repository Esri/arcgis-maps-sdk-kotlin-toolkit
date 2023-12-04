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

package com.arcgismaps.toolkit.mapviewidentifyapp.screens

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val mapViewProxy = remember {
        MapViewProxy()
    }
    val scope = rememberCoroutineScope()
    var showProgressIndicator by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetScaffoldState()
    var identifiedAttributes: Map<String, Any?> = remember { emptyMap() }
    val featureLayer = remember {
        FeatureLayer.createWithItem(PortalItem("https://www.arcgis.com/home/item.html?id=9e2f2b544c954fda9cd13b7f3e6eebce"))
    }
    LaunchedEffect(key1 = featureLayer) {
        showProgressIndicator = true
        featureLayer.load().onSuccess {
            sheetState.bottomSheetState.expand()
            showProgressIndicator = false
        }
    }
    val arcGISMap by remember {
        mutableStateOf(ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            operationalLayers.add(featureLayer)
        })
    }
    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
            EventDetails(showProgressIndicator, identifiedAttributes)
        }
    ) { paddingValues ->
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            arcGISMap = arcGISMap,
            mapViewProxy = mapViewProxy,
            selectionProperties = SelectionProperties(color = com.arcgismaps.Color.white),
            onSingleTapConfirmed = {
                scope.launch {
                    showProgressIndicator = true
                    identifiedAttributes = emptyMap()
                    featureLayer.clearSelection()
                    val result = mapViewProxy.identify(featureLayer, it.screenCoordinate, 20.0)
                    result.onSuccess {
                        val geoElement = it.geoElements.firstOrNull()
                        Log.e("Identify", "Identify found a geoelement")
                        geoElement?.attributes?.let {
                            identifiedAttributes = it
                        }
                        sheetState.bottomSheetState.expand()
                        (geoElement as? Feature)?.let {
                            featureLayer.selectFeature(it)
                        }
                    }
                    showProgressIndicator = false
                }
            }
        )
    }
}

@Composable
private fun EventDetails(
    showProgressIndicator: Boolean,
    identifiedAttributes: Map<String, Any?>
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Event Details",
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        if (showProgressIndicator) {
            CircularProgressIndicator(Modifier.padding(8.dp))
        } else if (identifiedAttributes.isEmpty()) {
            Text(
                "No attributes found.",
                Modifier.padding(8.dp)
            )
        } else {
            LazyColumn {
                items(identifiedAttributes.size) { i ->
                    val entry = identifiedAttributes.entries.elementAt(i)
                    AttributeRow(entry)
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun AttributeRow(entry: Map.Entry<String, Any?>) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Text(
            entry.key,
            Modifier
                .border(Dp.Hairline, Color.Black)
                .weight(0.3f)
                .padding(8.dp)
                .fillMaxHeight(),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            entry.value.toString(),
            Modifier
                .border(Dp.Hairline, Color.Black)
                .weight(0.7f)
                .padding(8.dp)
                .fillMaxHeight()
        )
    }
}
