package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.WrapAroundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * - Set a property
 * - Read a property
 * - GeoView operation that returns a result
 * - Be notified about GeoView events
 */


public class MapProperties() {
    public var arcGISMap: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
    public var wrapAroundMode: WrapAroundMode? = null
}

public sealed interface MapState {
    public val mapProperties: MapProperties
    public val onSingleTapConfirmed: StateFlow<SingleTapConfirmedEvent?>
}

public fun MapState(
    mapView: MapView,
    coroutineScope: CoroutineScope,
    mapProperties: MapProperties,
): MapState = MapStateImpl(mapView, coroutineScope, mapProperties)

private class MapStateImpl(
    var mapView: MapView,
    coroutineScope: CoroutineScope,
    override var mapProperties: MapProperties,
) : MapState {

    private val _onSingleTapConfirmed: MutableStateFlow<SingleTapConfirmedEvent?> =
        MutableStateFlow(null)
    override val onSingleTapConfirmed: StateFlow<SingleTapConfirmedEvent?> =
        _onSingleTapConfirmed.asStateFlow()

    init {
        coroutineScope.launch {
            mapView.onSingleTapConfirmed.collect {
                _onSingleTapConfirmed.value = it
            }
        }

        coroutineScope.launch {
            mapProperties.arcGISMap.collect {
                mapView.map = it
            }
        }
    }
}