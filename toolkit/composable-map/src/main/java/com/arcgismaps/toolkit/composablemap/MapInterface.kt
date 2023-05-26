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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public data class MapInsets(
    var start: Double = 0.0,
    var end: Double = 0.0,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)

public interface MapEvents {
    context(MapView, CoroutineScope) public fun onDown(downEvent: DownEvent) {}
    context(MapView, CoroutineScope) public fun onUp(upEvent: UpEvent) {}
    context(MapView, CoroutineScope) public fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {}
    context(MapView, CoroutineScope) public fun onDoubleTap(doubleTapEvent: DoubleTapEvent) {}
    context(MapView, CoroutineScope) public fun onLongPress(longPressEvent: LongPressEvent) {}
    context(MapView, CoroutineScope) public fun onTwoPointerTap(twoPointerTapEvent: TwoPointerTapEvent) {}
    context(MapView, CoroutineScope) public fun onPan(panEvent: PanChangeEvent) {}
}

public interface MapInterface : MapEvents {
    public val map: StateFlow<ArcGISMap>
    public val insets: StateFlow<MapInsets>
    public val currentViewpoint : StateFlow<Viewpoint?>
}
