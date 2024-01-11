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

package com.arcgismaps.toolkit.sceneviewcameracontrollerapp.screens

import androidx.lifecycle.ViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay

class SceneViewModel : ViewModel() {
    private val surfaceUrl =
        "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"

    private val simpleSceneSymbol = SimpleMarkerSceneSymbol(style = SimpleMarkerSceneSymbolStyle.Sphere, color = Color.red)

    private val craterLocation = Point(-109.929589, 38.437304, 1700.0, SpatialReference.wgs84())

    val simpleGraphic = Graphic(Point(craterLocation.x, craterLocation.y, 5000.0, craterLocation.spatialReference), simpleSceneSymbol)

    val sceneGraphicsOverlay = GraphicsOverlay().apply {
        this.graphics.add(simpleGraphic)
    }

    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        val elevationSource = ArcGISTiledElevationSource(surfaceUrl)
        this.baseSurface.elevationSources.add(elevationSource)
    }
}

