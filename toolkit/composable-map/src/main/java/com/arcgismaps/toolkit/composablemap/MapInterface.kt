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
     * Callback event for when the orientation of the [ComposableMap] changes with the given
     * [rotation]
     */
    public fun onMapRotationChanged(rotation: Double)

    /**
     * Callback event for the when the viewpoint of the [ComposableMap] changes with the given
     * [viewpoint]
     */
    public fun onMapViewpointChanged(viewpoint: Viewpoint)

    /**
     * Sets the [ComposableMap] current viewpoint to the given [viewpoint]
     */
    public fun setViewpoint(viewpoint: Viewpoint)

    /**
     * Sets the [ComposableMap] current viewpoint's rotation to the given [angleDegrees]
     */
    public fun setViewpointRotation(angleDegrees: Double)

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
    public val viewpoint: StateFlow<Viewpoint?>

    /**
     * The current rotation value of the [ComposableMap].
     */
    public val mapRotation: StateFlow<Double>
}

public class MapInterfaceImpl(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : MapInterface {

    private val _map : MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map : StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets : MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint : MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    override val viewpoint: StateFlow<Viewpoint?> = _viewpoint.asStateFlow()

    private val _mapRotation: MutableStateFlow<Double> = MutableStateFlow(0.0)
    override val mapRotation: StateFlow<Double> = _mapRotation.asStateFlow()

    override fun onMapRotationChanged(rotation: Double) {
        //TODO("Not yet implemented")
    }

    override fun onMapViewpointChanged(viewpoint: Viewpoint) {
        //TODO("Not yet implemented")
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        _viewpoint.value = viewpoint
    }

    override fun setViewpointRotation(angleDegrees: Double) {
        _mapRotation.value = angleDegrees
    }

    override fun setInsets(mapInsets: MapInsets) {
        _insets.value = mapInsets
    }

    override fun setMap(map: ArcGISMap) {
        _map.value = map
    }
}
