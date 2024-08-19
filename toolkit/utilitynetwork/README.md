

# UtilityNetwork

## Description

The UtilityNetwork toolkit component provides composable UI components for interacting with [UtilityNetworks](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.utilitynetworks/-utility-network/index.html?query=class%20UtilityNetwork).

Screenshot TBA
<!-- ![Screenshot](screenshot.png) -->

## Behavior

To see UtilityNetworkTraces in action, check out the [UtilityNetworkTrace microapp](../../microapps/UtilityNetworkTraceApp).

## Features

The `Trace` function is a composable function that provides configuration of networks traces and selects the Features that participate in the trace on a composable MapView.
- It can be integrated into any custom layout or container. The [microapp](../../microapps/UtilityNetworkTraceApp) integrates it into a `BottomSheet`.
- It follows the material 3 design system.

## Get Started

To get started, set up a `composable MapView` as described [here](../geoview-compose/README.md).

Ensure the MapView's [ArcGISMap]([GeoElements](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping/-arc-g-i-s-map/index.html?query=class%20ArcGISMap) has at least one UtilityNetwork in its definition.

```kotlin
// set up some variables
val mapViewProxy = rememeber { MapViewProxy() }
val scope = rememberCoroutineScope()
var popup: Popup? by remember { mutableStateOf(null) }

// a public webmap with UtilityNetworks defined
val portalItem = PortalItem(
    Portal.arcGISOnline(Portal.Connection.Anonymous),
    "471eb0bf37074b1fbb972b1da70fb310"
)
val arcGISMap = remember { ArcGISMap(portalItem) }

// call the composable MapView
MapView(
    arcGISMap = arcGISMap,
    mapViewProxy = proxy
)
```
#### Rendering the composable Trace function

The `Trace tool` can be rendered within a composition by simply calling the `Trace` composable function). The Trace should be displayed in a container. It's visibility and the container are external and should be controlled by the calling Composable.

```kotlin
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.toolkit.utilitynetwork.Trace

@Composable  
fun MyComposable(utilityNetwork : UtilityNetwork) {  
    // a container  
    MyContainer(modifier = Modifier) {
    	// create a Trace Composable
        Trace(  
            // pass in the UtilityNetwork object  
            utilityNetwork = UtilityNetwork,
	        // control the layout using the modifier property  
	        modifier = Modifier.fillMaxSize()
	    )  
    }  
} 
```  

#### Updating the `UtilityNetwork`

To display a new `UtilityNetwork` object, simply trigger a recomposition with the new `UtilityNetwork` object.

```kotlin  
@Composable  
fun MyComposable(viewModel : MyViewModel) {  
    // use a state object that will recompose this composable when the UtilityNetwork changes
    // in this example, the UtilityNetwork object is hoisted in the ViewModel
    val UtilityNetwork : State by viewModel.UtilityNetwork  
    // a container  
    MyContainer(modifier = Modifier) {
        Trace(    
	        utilityNetwork = UtilityNetwork,  
	        modifier = Modifier.fillMaxSize()
	    )  
    }  
}
```  

More information on the material 3 specs [here](https://m3.material.io/components/text-fields/specs#e4964192-72ad-414f-85b4-4b4357abb83c)
