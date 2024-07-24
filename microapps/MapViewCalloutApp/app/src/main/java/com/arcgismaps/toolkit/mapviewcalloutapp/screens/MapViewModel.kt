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

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.arcgismaps.mapping.GeoElement
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
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    val mapViewProxy = MapViewProxy()

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
    }

    val arcGISMapWithFeatureLayer = ArcGISMap(
        uri = "https://www.arcgis.com/home/item.html?id=16f1b8ba37b44dc3884afc8d5f454dd2"
    ).apply {
        initialViewpoint = Viewpoint(
            Point(x = -1.3659e7, y = 5.6917e6),
            scale = 50000.0,
        )
    }

    private val _mapPoint = MutableStateFlow<Point?>(null)
    val mapPoint: StateFlow<Point?> = _mapPoint.asStateFlow()

    private val _selectedGeoElement = MutableStateFlow<GeoElement?>(null)
    val selectedGeoElement: StateFlow<GeoElement?> = _selectedGeoElement.asStateFlow()

    private val _selectedLayerName = MutableStateFlow("")
    val selectedLayerName: StateFlow<String> = _selectedLayerName.asStateFlow()

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation.asStateFlow()

    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    val tapLocationGraphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    val customGraphicsOverlay = listOf(
        makeRenderedPointGraphicsOverlay(),
        makeRenderedLineGraphicsOverlay(),
        makeRenderedPolygonGraphicsOverlay(),
        makeRenderedCurvedPolygonGraphicsOverlay(),
        makeRenderedEllipseGraphicsOverlay()
    )

    private var currentIdentifyJob: Job? = null

    fun clearMapPoint() {
        _mapPoint.value = null
        tapLocationGraphicsOverlay.graphics.clear()
    }

    fun setOffset(offset: Offset) {
        _offset.value = offset
    }

    fun setMapPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        _mapPoint.value = singleTapConfirmedEvent.mapPoint

        tapLocationGraphicsOverlay.graphics.clear()
        tapLocationGraphicsOverlay.graphics.add(
            Graphic(
                geometry = _mapPoint.value,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    fun clearTapLocationAndGeoElement() {
        _tapLocation.value = null
        _selectedGeoElement.value = null
        tapLocationGraphicsOverlay.graphics.clear()
    }

    fun setTapLocation(tapLocation: Point?, nullTapLocation: Boolean) {
        _tapLocation.value = if (nullTapLocation) null else tapLocation

        tapLocationGraphicsOverlay.graphics.clear()
        tapLocationGraphicsOverlay.graphics.add(
            Graphic(
                geometry = tapLocation,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The
     * identified geoelement is set to [_selectedGeoElement].
     *
     * @since 200.5.0
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = viewModelScope.launch {
            val result = mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 1.dp
            )
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    _selectedGeoElement.value = identifyLayerResultList[0].geoElements.firstOrNull()
                    _selectedLayerName.value = identifyLayerResultList[0].layerContent.name
                }
            }
        }
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The
     * identified geoelement is set to [_selectedGeoElement].
     *
     * @since 200.5.0
     */
    fun identifyGraphicsOverlays(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = viewModelScope.launch {
            val result = mapViewProxy.identifyGraphicsOverlays(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 5.dp
            )
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    _selectedGeoElement.value = identifyLayerResultList[0].geoElements.firstOrNull()
                } else{
                    _selectedGeoElement.value = null
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    /**
     * Recenter the viewpoint to the given [mapPoint]
     */
    fun recenterMap(mapPoint: Point?) {
        viewModelScope.launch {
            mapViewProxy.setViewpointAnimated(
                viewpoint = Viewpoint(mapPoint!!)
            )
        }
    }


    /**
     * Make a point, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedPointGraphicsOverlay(): GraphicsOverlay {
        // create point
        val pointGeometry = Point(40e5, 40e5, SpatialReference.webMercator())
        // create graphic for point
        val pointGraphic = Graphic(pointGeometry).apply {
            attributes["graphicType"] = "Point Graphic"
        }
        // red diamond point symbol
        val pointSymbol =
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 10f)
        // create simple renderer
        val pointRenderer = SimpleRenderer(pointSymbol)
        // create a new graphics overlay with these settings and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(pointGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = pointRenderer
        }
    }

    /**
     * Create a polyline, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedLineGraphicsOverlay(): GraphicsOverlay {
        // create line
        val lineBuilder = PolylineBuilder(SpatialReference.webMercator()) {
            addPoint(-10e5, 40e5)
            addPoint(20e5, 50e5)
        }
        // create graphic for polyline
        val lineGraphic = Graphic(lineBuilder.toGeometry()).apply {
            attributes["graphicType"] = "Line Graphic"
        }
        // solid blue line symbol
        val lineSymbol =
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.blue, 5f)
        // create simple renderer
        val lineRenderer = SimpleRenderer(lineSymbol)

        // create graphic overlay for polyline and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(lineGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = lineRenderer
        }
    }

    /**
     * Create a polygon, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedPolygonGraphicsOverlay(): GraphicsOverlay {
        // create polygon
        val polygonBuilder = PolygonBuilder(SpatialReference.webMercator()) {
            addPoint(-20e5, 20e5)
            addPoint(20e5, 20e5)
            addPoint(20e5, -20e5)
            addPoint(-20e5, -20e5)
        }
        // create graphic for polygon
        val polygonGraphic = Graphic(polygonBuilder.toGeometry()).apply {
            attributes["graphicType"] = "Polygon Graphic"
        }
        // solid yellow polygon symbol
        val polygonSymbol =
            SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.yellow, null)
        // create simple renderer
        val polygonRenderer = SimpleRenderer(polygonSymbol)

        // create graphic overlay for polygon and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(polygonGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = polygonRenderer
        }
    }

    /**
     * Create a curved polygon, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedCurvedPolygonGraphicsOverlay(): GraphicsOverlay {
        // create a point for the center of the geometry
        val originPoint = Point(40e5, 5e5, SpatialReference.webMercator())
        // create polygon
        val curvedPolygonGeometry = makeHeartGeometry(originPoint, 10e5)
        // create graphic for polygon
        val polygonGraphic = Graphic(curvedPolygonGeometry).apply {
            attributes["graphicType"] = "Curved Polygon Graphic"
        }
        // create a simple fill symbol with outline
        val curvedLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f)
        val curvedFillSymbol =
            SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.red, curvedLineSymbol)
        // create simple renderer
        val polygonRenderer = SimpleRenderer(curvedFillSymbol)

        // create graphic overlay for polygon and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(polygonGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = polygonRenderer
        }
    }

    /**
     * Create a heart-shape geometry with Bezier and elliptic arc segments from a given [center]
     * point and [sideLength].
     */
    private fun makeHeartGeometry(center: Point, sideLength: Double): Geometry {
        val spatialReference = center.spatialReference
        // the x and y coordinates to simplify the calculation
        val minX = center.x - 0.5 * sideLength
        val minY = center.y - 0.5 * sideLength
        // the radius of the arcs
        val arcRadius = sideLength * 0.25

        // bottom left curve
        val leftCurveStart = Point(center.x, minY, spatialReference)
        val leftCurveEnd = Point(minX, minY + 0.75 * sideLength, spatialReference)
        val leftControlPoint1 = Point(center.x, minY + 0.25 * sideLength, spatialReference)
        val leftControlPoint2 = Point(minX, center.y, spatialReference)
        val leftCurve = CubicBezierSegment(
            leftCurveStart,
            leftControlPoint1,
            leftControlPoint2,
            leftCurveEnd,
            spatialReference
        )

        // top left arc
        val leftArcCenter =
            Point(minX + 0.25 * sideLength, minY + 0.75 * sideLength, spatialReference)
        val leftArc = EllipticArcSegment.createCircularEllipticArc(
            leftArcCenter,
            arcRadius,
            Math.PI,
            -Math.PI,
            spatialReference
        )

        // top right arc
        val rightArcCenter =
            Point(minX + 0.75 * sideLength, minY + 0.75 * sideLength, spatialReference)
        val rightArc = EllipticArcSegment.createCircularEllipticArc(
            rightArcCenter,
            arcRadius,
            Math.PI,
            -Math.PI,
            spatialReference
        )

        // bottom right curve
        val rightCurveStart = Point(minX + sideLength, minY + 0.75 * sideLength, spatialReference)
        val rightControlPoint1 = Point(minX + sideLength, center.y, spatialReference)
        val rightCurve = CubicBezierSegment(
            rightCurveStart,
            rightControlPoint1,
            leftControlPoint1,
            leftCurveStart,
            spatialReference
        )

        // create a mutable part list
        val heartParts = MutablePart.createWithSegments(
            listOf(leftCurve, leftArc, rightArc, rightCurve),
            spatialReference
        )
        // return the heart
        return Polygon(listOf(heartParts).asIterable())
    }

    /**
     * Create an ellipse, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedEllipseGraphicsOverlay(): GraphicsOverlay {
        // create and set all the parameters so that the ellipse has a major axis of 400 kilometres,
        // a minor axis of 200 kilometres and is rotated at an angle of -45 degrees
        val parameters = GeodesicEllipseParameters.createForPolygon().apply {
            axisDirection = -45.0
            angularUnit = AngularUnit.degrees
            center = Point(40e5, 23e5, SpatialReference.webMercator())
            linearUnit = LinearUnit.kilometers
            maxPointCount = 100L
            maxSegmentLength = 20.0
            semiAxis1Length = 200.0
            semiAxis2Length = 400.0
        }

        // define the ellipse parameters to a polygon geometry
        val polygon = GeometryEngine.ellipseGeodesicOrNull(parameters)
        // set the ellipse fill color
        val ellipseSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.magenta, null)
        // return the purple ellipse
        return GraphicsOverlay().apply {
            // add the symbol to the renderer and add it to the graphic overlay
            renderer = SimpleRenderer(ellipseSymbol)
            graphics.add(Graphic(polygon).apply {
                attributes["graphicType"] = "Eclipse Polygon Graphic"
            })
        }
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

    private val Color.Companion.yellow: Color
        get() {
            return fromRgba(255, 255, 0, 255)
        }

    private val Color.Companion.magenta: Color
        get() {
            return fromRgba(255, 0, 255, 255)
        }
}
