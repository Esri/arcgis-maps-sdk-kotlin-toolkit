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
import java.util.UUID

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
     * [rotation] and called by the [flowProducer]
     */
    public fun onMapRotationChanged(rotation: Double, flowProducer: UUID? = null) {}

    /**
     * Sets the [ComposableMap] current viewpoint's rotation to the given [angleDegrees]
     */
    public fun setViewpointRotation(angleDegrees: Double) {}

    /**
     * Callback event for the when the viewpoint of the [ComposableMap] changes with the given
     * [viewpoint] and called by the [flowProducer]
     */
    public fun onMapViewpointChanged(viewpoint: Viewpoint, flowProducer: UUID? = null) {}

    /**
     * Sets the [ComposableMap] current viewpoint to the given [viewpoint]
     */
    public fun setViewpoint(viewpoint: Viewpoint) {}
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
    public val viewpoint: StateFlow<FlowData<Viewpoint?>>

    /**
     * The current rotation value of the [ComposableMap].
     */
    public val mapRotation: StateFlow<FlowData<Double>>
}

/**
 * Data holder class for [data] for use with flows where the producer needs to uniquely identified.
 * In cases where an instance is both emitting and collecting on a flow, the [producer] property
 * can be used to match and drop collects to avoid introducing a feedback loop.
 */
public data class FlowData<T>(val data: T, val producer: UUID? = null)
