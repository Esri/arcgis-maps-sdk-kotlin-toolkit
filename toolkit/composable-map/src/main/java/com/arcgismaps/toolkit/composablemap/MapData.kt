package com.arcgismaps.toolkit.composablemap

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

public data class MapData(
    public var map: ArcGISMap,
    public var viewPoint: Viewpoint? = null,
    public var insets: MapInsets = MapInsets()
)

public data class MapInsets(
    var start: Double = 0.0,
    var end: Double = 0.0,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)

public interface MapInterface {
    /**
     * A container which contains rememberable state needed to recompose a [ComposableMap]
     */
    public val mapData: StateFlow<MapData>
    
    /**
     * A function to interact with [MapView] business logic. This function cannot directly update the
     * composable state of the [ComposableMap].
     *
     * This function runs in a `LaunchedEffect` and therefore is run in  the context of a CoroutineScope receiver.
     * This CoroutineScope is explicitly listed as a context receiver here, so that access to its Coroutine context
     * may be written into the body of this function's implementations.
     *
     * In addition, this function runs in a `LaunchedEffect` inside a call to `@Composable AndroidView` function
     * which wraps a [MapView], and access to the MapView is provided as a context receiver.
     * see https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
     *
     * @receiver MapView
     * @receiver CoroutineScope
     */
    context(MapView, CoroutineScope) public suspend fun viewLogic()
}
