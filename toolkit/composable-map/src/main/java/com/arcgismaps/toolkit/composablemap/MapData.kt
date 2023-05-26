package com.arcgismaps.toolkit.composablemap

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
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
     * This function runs in in the context of a [MapView]. Specifically, it runs in a call to
     * `@Composable AndroidView` which wraps and manages a MapView. Access to this MapView
     * is provided in this function `viewLogic` by listing MapView as a context receiver.
     * see https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
     *
     * @receiver MapView
     * @receiver CoroutineScope
     */
    context(MapView, CoroutineScope) public fun onDown(downEvent: DownEvent) {}
    context(MapView, CoroutineScope) public fun onUp(upEvent: UpEvent) {}
    context(MapView, CoroutineScope) public fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {}
    context(MapView, CoroutineScope) public fun onDoubleTap(doubleTapEvent: DoubleTapEvent) {}
    context(MapView, CoroutineScope) public fun onLongPress(longPressEvent: LongPressEvent) {}
    context(MapView, CoroutineScope) public fun onTwoPointerTap(twoPointerTapEvent: TwoPointerTapEvent) {}
    context(MapView, CoroutineScope) public fun onPan(panEvent: PanChangeEvent) {}
    
    
    
}
