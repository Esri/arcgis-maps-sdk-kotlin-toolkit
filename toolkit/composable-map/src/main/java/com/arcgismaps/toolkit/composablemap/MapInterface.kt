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

    /**
     * Sets the [ComposableMap] current viewpoint to the given [viewpoint]
     */
    public fun setViewpoint(viewpoint: Viewpoint, channel: Channel)

    /**
     * Sets the [ComposableMap] current viewpoint's rotation to the given [angleDegrees]
     */
    public fun setViewpointRotation(angleDegrees: Double, channel: Channel)

    /**
     * Sets the [ComposableMap] insets to the given [mapInsets]
     */
    public fun setInsets(mapInsets: MapInsets)

    /**
     * Sets the given [map] on the [ComposableMap]
     */
    public fun setMap(map: ArcGISMap)
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
    public val viewpoint: MapFlow<Viewpoint?>

    /**
     * The current rotation value of the [ComposableMap].
     */
    public val mapRotation: MapFlow<Double>
}

/**
 * Factory function for the default implementation of [MapInterface]
 */
public fun MapInterface(arcGISMap: ArcGISMap, mapInsets: MapInsets = MapInsets()): MapInterface =
    MapInterfaceImpl(arcGISMap, mapInsets)

/**
 * A default implementation for the [MapInterface]
 */

public class MapInterfaceImpl(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : MapInterface {

    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint: MutableMapFlow<Viewpoint?> = MutableMapFlow(null)
    override val viewpoint: MapFlow<Viewpoint?> = _viewpoint

    private val _mapRotation: MutableMapFlow<Double> = MutableMapFlow(0.0)
    override val mapRotation: MapFlow<Double> = _mapRotation

    override fun setViewpoint(viewpoint: Viewpoint, channel: Channel) {
        _viewpoint.setValue(viewpoint, channel)
    }

    override fun setViewpointRotation(angleDegrees: Double, channel: Channel) {
        _mapRotation.setValue(angleDegrees, channel)
    }

    override fun setInsets(mapInsets: MapInsets) {
        _insets.value = mapInsets
    }

    override fun setMap(map: ArcGISMap) {
        _map.value = map
    }
}
