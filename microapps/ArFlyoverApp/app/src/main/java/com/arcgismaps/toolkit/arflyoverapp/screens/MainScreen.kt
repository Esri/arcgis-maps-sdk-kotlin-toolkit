/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.arflyoverapp.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.IntegratedMeshLayer
import com.arcgismaps.toolkit.ar.FlyoverSceneView
import com.arcgismaps.toolkit.ar.rememberFlyoverSceneViewProxy

@Composable
fun MainScreen() {

    val elevationSource =
        ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")

    val meshLayer =
        IntegratedMeshLayer("https://tiles.arcgis.com/tiles/z2tnIkrLQ2BRzr6P/arcgis/rest/services/Girona_Spain/SceneServer")

    val arcGISScene by remember {
        mutableStateOf(
            ArcGISScene(BasemapStyle.ArcGISImagery).apply {
                baseSurface.elevationSources.add(elevationSource)
                operationalLayers.add(meshLayer)
            }
        )
    }

    val initialLocation =
        Point(
            2.82407,
            41.99101,
            230.0,
            SpatialReference.wgs84()
        )
    val initialHeading = 160.0

    val flyoverSceneViewProxy = rememberFlyoverSceneViewProxy(
        initialLocation = initialLocation,
        initialHeading = initialHeading,
        translationFactor = 1000.0
    )

    FlyoverSceneView(
        arcGISScene = arcGISScene,
        flyoverSceneViewProxy = flyoverSceneViewProxy
    )

    Button(onClick = {
        flyoverSceneViewProxy.setLocationAndHeading(
//            location = Point(
//                -3.200833,
//                55.948612,
//                150.0,
//                SpatialReference.wgs84()
//            ),
            location = initialLocation,
            heading = initialHeading
        )
    }) {
        Text("Change parameters")
    }
}