package com.arcgismaps.toolkit.composablemap

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.flow.SharedFlow
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

public data class MapInsets(
    var start: Double = 0.0,
    var end: Double = 0.0,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)

/**
 * An interface for providing business logic to ComposableMap touch events.
 * These methods are called in the [ComposableMap] function in the context of
 * a [MapView] and a `CoroutineScope`, their public API are available
 * to use in implementations of these functions with no reference required.
 *
 * ```
 * context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
 *     // launch will be called on the CoroutineScope provided by context. This CoroutineScope will be cancelled
 *     // when the ComposableMap leaves the composition.
 *     launch {
 *         // setBookmark will be called on the MapView provided by context.
 *         setBookmark(null)
 *     }
 * }
 * ```
 *
 * @see ComposableMap
 */
public interface MapEvents {
    /**
     * Support for down events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onDown(downEvent: DownEvent) {}

    /**
     * Support for up events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onUp(upEvent: UpEvent) {}
    
    /**
     * Support for single tap events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {}
    
    /**
     * Support for double tap events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onDoubleTap(doubleTapEvent: DoubleTapEvent) {}
    
    /**
     * Support for long press events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onLongPress(longPressEvent: LongPressEvent) {}
    
    /**
     * Support for two pointer tap events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onTwoPointerTap(twoPointerTapEvent: TwoPointerTapEvent) {}
    
    /**
     * Support for pan events on [ComposableMap]
     */
    context(MapView, CoroutineScope) public fun onPan(panEvent: PanChangeEvent) {}

    public fun onMapRotationChanged(rotation: Double) {}
}

/**
 * An interface for consumption by [ComposableMap]. This interface represents the state needed
 * for [ComposableMap] to re/compose.
 */
public interface MapInterface : MapEvents {
    /**
     * The model for [ComposableMap]
     */
    public val map: StateFlow<ArcGISMap>
    
    /**
     * Insets to apply to the Box which contains the [ComposableMap]
     */
    public val insets: StateFlow<MapInsets>
    
    /**
     * The [Viewpoint] from which the [ComposableMap] is drawn.
     */
    public val currentViewpoint : StateFlow<Viewpoint?>
    public val resetMapRotation : SharedFlow<Unit>
}
