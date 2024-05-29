package com.arcgismaps.toolkit.mapviewcalloutapp

import androidx.core.graphics.rotationMatrix
import androidx.lifecycle.ViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MapViewModel : ViewModel() {
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
    }

    private val _mapPoint = MutableStateFlow<Point?>(null)
    val mapPoint: StateFlow<Point?> = _mapPoint

    val tapLocationGraphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    fun setMapPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        _mapPoint.value = singleTapConfirmedEvent.mapPoint

        tapLocationGraphicsOverlay.graphics.clear()
        tapLocationGraphicsOverlay.graphics.add(
            Graphic(
                geometry = _mapPoint.value,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }
}