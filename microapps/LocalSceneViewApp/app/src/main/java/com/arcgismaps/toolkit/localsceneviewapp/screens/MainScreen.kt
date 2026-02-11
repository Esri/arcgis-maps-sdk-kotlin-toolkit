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

package com.arcgismaps.toolkit.localsceneviewapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneViewingMode
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView

@Composable
fun MainScreen() {

    val camera = remember {
        Camera(
            locationPoint = Point(
                19455578.6821,
                -5056336.2227,
                1699.3366,
                SpatialReference.webMercator()
            ),
            heading = 338.7410,
            pitch = 40.3763,
            roll = 0.0,
        )
    }

    val sceneLayer = remember {
        ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=61da8dc1a7bc4eea901c20ffb3f8b7af")
    }

    val elevationSource = remember {
        ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
    }

    val arcGISScene = remember {
        ArcGISScene(
            viewingMode = SceneViewingMode.Local,
            basemapStyle = BasemapStyle.ArcGISTopographic
        ).apply {
            operationalLayers.add(sceneLayer)
            baseSurface.elevationSources.add(elevationSource)
            initialViewpoint = Viewpoint(
                center = Point(19455026.8116, -5054995.7415, SpatialReference.webMercator()),
                scale = 8314.6991,
                camera = camera
            )
            clippingArea = Envelope(
                xMin = 19454578.8235,
                yMin = -5055381.4798,
                xMax = 19455518.8814,
                yMax = -5054888.4150,
                spatialReference = SpatialReference.webMercator()
            )
            isClippingEnabled = true
        }
    }

    LocalSceneView(
        scene = arcGISScene,
        modifier = Modifier.fillMaxSize()
    )
}
