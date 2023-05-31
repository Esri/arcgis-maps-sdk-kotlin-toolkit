package com.arcgismaps.toolkit.composablemap

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.flow.StateFlow

public data class MapInsets(
    var start: Double = 0.0,
    var end: Double = 0.0,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)

public interface MapEvents {
    public fun onSingleTapConfirmed(event: SingleTapConfirmedEvent)
    public fun onMapRotationChanged(rotation: Double)
    public fun onMapRotationReset()
}

public interface MapInterface : MapEvents {
    public val map: StateFlow<ArcGISMap>
    public val insets: StateFlow<MapInsets>
    public val currentViewpoint : StateFlow<Viewpoint?>
    public val resetMapRotation : StateFlow<Boolean>
}
