package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.ArcGISMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MapState interface of MapView's flow events and it's properties
 */
public sealed interface MapState : GeoComposeState {
    public val arcGISMap: StateFlow<ArcGISMap?>
}

public fun MapState(arcGISMap: ArcGISMap? = null): MapState = MapStateImpl(arcGISMap)

private class MapStateImpl(arcGISMap: ArcGISMap?) : MapState, GeoComposeStateImpl() {

    private val _arcGISMap: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
    override val arcGISMap: StateFlow<ArcGISMap?> = _arcGISMap.asStateFlow()

    init {
        _arcGISMap.value = arcGISMap
    }
}