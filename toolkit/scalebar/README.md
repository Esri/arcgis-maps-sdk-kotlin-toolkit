# Scalebar
A Scalebar visually represents precise linear measurements on a map, aiding users in gauging the size of features or distances between them. It typically consists of a divided line or bar labeled with ground lengths, often in multiples of map units like tens of kilometers or hundreds of miles.

![Screenshot](screenshot.png)

## Features

- Configurable to show as either a bar or line, offering distinct styles for each.
- Customizable with personalized colors for fills, lines, shadows, and text.
- Option to automatically hide after a pan or zoom action.
- Shows both metric and imperial units.

## Behavior

- The scale bar utilizes geodetic computations to offer precise measurements for maps of any spatial reference. 
- The accuracy of the measurement is centered on the displayed map extent. Consequently, at smaller scales (when zoomed out), there may be some inaccuracies at the edges of the visible extent. 
- As the map is panned and zoomed, the scale bar dynamically adjusts its size and updates the measurement according to the updated map extent.

To see it in action, check out the [microapp](../../microapps/ScalebarApp).

## Usage

*View the API Reference for the `scalebar` module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.scalebar/index.html).*

### Basic usage for displaying a `Scalebar` on a `MapView`

The simplest workflow is to display the `Scalebar` composable over the top of a composable `MapView` using a `Box` 

```kotlin
    // Changes in the `viewpoint`, `unitsPerDip` and `spatialReference`
    // need to be provided from the MapView to the `Scalebar`.
    // So it can recompose and show accurate values
    // whenever their value change due to panning and zooming on the `MapView`
    var viewpoint: Viewpoint? by remember { mutableStateOf(null) }
    var unitsPerDip by remember { mutableDoubleStateOf(Double.NaN) }
    var spatialReference: SpatialReference? by remember { mutableStateOf(null) }
    // show composable MapView with a Scalebar
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
            onSpatialReferenceChanged = { spatialReference = it },
            onUnitsPerDipChanged = { unitsPerDip = it },
            onViewpointChangedForCenterAndScale = { viewpoint = it }
        )
        
        Scalebar(
            modifier = Modifier
                .padding(25.dp)
                .align(Alignment.BottomStart),
            maxWidth = 175.0,
            unitsPerDip = unitsPerDip,
            viewpoint = viewpoint,
            spatialReference = spatialReference,
            style = ScalebarStyle.AlternatingBar,
        )
    }
```
