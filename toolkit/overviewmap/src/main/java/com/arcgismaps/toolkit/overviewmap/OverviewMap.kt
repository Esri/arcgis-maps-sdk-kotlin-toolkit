/*
 COPYRIGHT 1995-2025 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

package com.arcgismaps.toolkit.overviewmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

@Composable
public fun OverviewMapForMapView(
    viewpoint: Viewpoint?,
    visibleArea: Polygon?,
    symbol: Symbol = remember {
        SimpleFillSymbol(
            outline = SimpleLineSymbol(color = Color.red),
            color = Color.transparent
        )
    },
    scaleFactor: Double = 25.0,
    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    modifier: Modifier = Modifier
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        visibleArea = visibleArea,
        scaleFactor = scaleFactor,
        fillSymbol = symbol,
        map = map,
        modifier = modifier
    )
}

@Composable
public fun OverviewMapForSceneView(
    viewpoint: Viewpoint?,
    scaleFactor: Double = 25.0,
    symbol: Symbol = remember {
        SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.red
        )
    },
    map: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    modifier: Modifier = Modifier
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        scaleFactor = scaleFactor,
        markerSymbol = symbol,
        map = map,
        modifier = modifier
    )
}

@Composable
private fun OverviewMapImpl(
    viewpoint: Viewpoint?,
    map: ArcGISMap,
    visibleArea: Polygon? = null,
    markerSymbol: Symbol? = null,
    fillSymbol: Symbol? = null,
    scaleFactor: Double = 25.0,
    modifier: Modifier = Modifier,
) {
    val proxy = remember {
        MapViewProxy()
    }

    val graphic = remember {
        Graphic()
    }

    val graphicsOverlay = remember {
        GraphicsOverlay(listOf(graphic))
    }

    val graphicsOverlays = remember {
        listOf(graphicsOverlay)
    }

    viewpoint?.let {
        if (viewpoint.viewpointType == ViewpointType.CenterAndScale) {
            proxy.setViewpoint(
                Viewpoint(
                    center = it.targetGeometry as Point,
                    scale = it.targetScale * scaleFactor
                )
            )
            if (visibleArea == null) {
                graphic.geometry = it.targetGeometry
                graphic.symbol = markerSymbol
            } else {
                graphic.geometry = visibleArea
                graphic.symbol = fillSymbol
            }
        }
    }

    MapView(
        modifier = modifier,
        arcGISMap = map,
        mapViewProxy = proxy,
        graphicsOverlays = graphicsOverlays,
        isAttributionBarVisible = false,
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
    )
}