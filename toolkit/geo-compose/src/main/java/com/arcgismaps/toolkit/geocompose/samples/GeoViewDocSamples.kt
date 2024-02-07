package com.arcgismaps.toolkit.geocompose.samples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.SceneView

/**
 * @suppress Suppress this function from being indexed in the KDoc
 */
@Composable
public fun SceneViewSample() {
    // Display a SceneView with an elevation surface and an initial viewpoint

    // add base surface for elevation data
    val elevationSource = ArcGISTiledElevationSource(
        uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    )
    val surface = Surface().apply {
        elevationSources.add(elevationSource)
        // add an exaggeration factor to increase the 3D effect of the elevation.
        elevationExaggeration = 2.5f
    }

    val cameraLocation = Point(
        x = -118.794,
        y = 33.909,
        z = 5330.0,
        spatialReference = SpatialReference.wgs84()
    )

    val camera = Camera(
        locationPoint = cameraLocation,
        heading = 355.0,
        pitch = 72.0,
        roll = 0.0
    )

    // display the Composable SceneView
    SceneView(
        modifier = Modifier.fillMaxSize(),
        arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface = surface
            initialViewpoint = Viewpoint(cameraLocation, camera)
        }
    )
}


/**
 * @suppress Suppress this function from being indexed in the KDoc
 */
@Composable
public fun MapViewSample() {
    // Display a feature layer MapView using a service feature table

    // map used to display a feature layer
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint( // USA viewpoint
            center = Point(-11e6, 5e6, SpatialReference.webMercator()),
            scale = 1e8
        )
    }

    // create a service feature table and a feature layer from it
    val serviceFeatureTable = ServiceFeatureTable(
        uri = "https://services.arcgis.com/jIL9msH9OI208GCb/arcgis/rest/services/USA_Daytime_Population_2016/FeatureServer/0"
    )

    // create the feature layer using the service feature table
    val featureLayer: FeatureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)

    // use symbol to show U.S. states with a black outline
    val lineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.black,
        width = 1.0f
    )

    // set feature layer properties
    featureLayer.apply {
        // set renderer for the feature layer
        renderer = SimpleRenderer(lineSymbol)
        opacity = 0.8f
        maxScale = 10000.0
    }
    // add the feature layer to the map's operational layers
    map.operationalLayers.add(featureLayer)

    // display the Composable MapView
    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = map
    )
}