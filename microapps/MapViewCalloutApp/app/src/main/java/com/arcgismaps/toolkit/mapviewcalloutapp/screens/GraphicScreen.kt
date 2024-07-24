/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.CubicBezierSegment
import com.arcgismaps.geometry.EllipticArcSegment
import com.arcgismaps.geometry.GeodesicEllipseParameters
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.MutablePart
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun GraphicScreen(mapViewModel: MapViewModel) {
    Box {

        // This causes unnecessary MapView recompositions.
        // TODO: val selectedGeoelement = mapViewModel.selectedGeoElement.collectAsState().value

        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                initialViewpoint = Viewpoint(15.169193, 16.333479, 1e8)
            },
            mapViewProxy = mapViewModel.mapViewProxy,
            graphicsOverlays = rememberSaveable { mapViewModel.customGraphicsOverlay },
            onSingleTapConfirmed = mapViewModel::identifyGraphicsOverlays
        ) {

            // This works only with LaunchedEffect(geoElement) in CalloutInternal
            val selectedGeoelement = mapViewModel.selectedGeoElement.collectAsState().value

            if (selectedGeoelement != null) {
                Callout(geoElement = selectedGeoelement) {
                    Text(text = "${selectedGeoelement.attributes}")
                }
            }
        }
    }
}
