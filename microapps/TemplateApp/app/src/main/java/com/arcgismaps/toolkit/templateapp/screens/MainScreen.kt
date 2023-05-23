package com.arcgismaps.toolkit.templateapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun MainScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val mapData = MapData(map).apply {
        viewPoint = Viewpoint(39.8, -98.6, 10e7)
        insets = MapInsets(bottom = 25.0)
    }
    val mapViewModel = MapViewModel(mapData)
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )
}

class MapViewModel(
    mapData: MapData
) : ViewModel(), MapInterface {
    private val _mapData: MutableStateFlow<MapData> = MutableStateFlow(mapData)
    override val mapData = _mapData.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {}
}
