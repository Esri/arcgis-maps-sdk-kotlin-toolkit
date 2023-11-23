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

package com.arcgismaps.toolkit.featureeditorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.Feature
import com.arcgismaps.data.FeatureCollection
import com.arcgismaps.data.FeatureCollectionTable
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureCollectionLayer
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureeditorapp.screens.FeatureEditorApp
import com.arcgismaps.toolkit.featureeditorapp.screens.FeatureEditorAppState
import com.arcgismaps.toolkit.featureeditorapp.ui.theme.FeatureEditorAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
            ?: throw IllegalStateException("Could not create API key for app.")


        val state = FeatureEditorAppState(MapState())

        lifecycleScope.launch {

            val (map, feature) = createMapWithFeature()
            state.mapState.setMap(map)
        }

        setContent {
            FeatureEditorAppTheme {
                FeatureEditorApp(state)
            }
        }
    }
}

private suspend fun createMapWithFeature(): Pair<ArcGISMap, Feature> {
    val table = FeatureCollectionTable(
        fields = listOf(
            Field(FieldType.Text, "name", "ye olde name", 64, isNullable = false),
            Field(FieldType.Int32, "age", "age", 64, isNullable = false)
        ),
        geometryType = GeometryType.Point,
        spatialReference = SpatialReference.wgs84()
    )
    val collection = FeatureCollection(listOf(table))

    val feature = table.createFeature(mapOf("name" to "The Great Tree", "age" to 763), Point(0.0, 0.0, SpatialReference.wgs84()))

    table.addFeature(feature)

    val map =  ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
        operationalLayers.add(FeatureCollectionLayer(collection))
    }

    return Pair(map, feature)
}
