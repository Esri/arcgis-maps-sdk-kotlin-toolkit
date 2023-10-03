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
 * Properties of the MapView which are listed as MutableStateFlows
 */
public class MapProperties(
    arcGISMap: ArcGISMap? = null,
    wrapAroundMode: WrapAroundMode? = WrapAroundMode.EnabledWhenSupported
) {
    public val arcGISMap: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
    public val wrapAroundMode: MutableStateFlow<WrapAroundMode?> = MutableStateFlow(null)

    init {
        this.arcGISMap.value = arcGISMap
        this.wrapAroundMode.value = wrapAroundMode
    }
}

/**
 * MapState interface of MapView's flow events and it's properties
 */
public sealed interface MapState : GeoState {
    public val mapProperties: MapProperties
    public val onSingleTapConfirmed: StateFlow<SingleTapConfirmedEvent?>
    public val mapRotation: StateFlow<Double>
}

public fun MapState(
    coroutineScope: CoroutineScope,
    mapProperties: MapProperties,
): MapState = MapStateImpl(coroutineScope, mapProperties)

private class MapStateImpl(
    coroutineScope: CoroutineScope,
    override var mapProperties: MapProperties,
) : MapState, GeoStateImpl(coroutineScope) {

    private val _onSingleTapConfirmed: MutableStateFlow<SingleTapConfirmedEvent?> =
        MutableStateFlow(null)
    override val onSingleTapConfirmed = _onSingleTapConfirmed.asStateFlow()

    private val _mapRotation: MutableStateFlow<Double> = MutableStateFlow(0.0)
    override val mapRotation = _mapRotation.asStateFlow()

    private var mapView: MapView? = null

    init {
        coroutineScope.launch {
            geoView.collect {
                it?.let { geoView ->
                    mapView = geoView as MapView // doesn't feel like an ideal approach to setting the MapView
                }
            }
        }
        coroutineScope.launch {
            mapView?.onSingleTapConfirmed?.collect {
                _onSingleTapConfirmed.value = it
            }
        }

        coroutineScope.launch {
            mapView?.mapRotation?.collect {
                _mapRotation.value = it
            }
        }

        coroutineScope.launch {
            mapProperties.arcGISMap.collect {
                mapView?.map = it
            }
        }

        coroutineScope.launch {
            mapProperties.wrapAroundMode.collect {
                it?.let {
                    mapView?.wrapAroundMode = it
                }
            }
        }
    }
}