package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.composablemap.ComposableMap

@Composable
fun MainScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    val mapRotation by mapViewModel.mapRotation.collectAsState(initial = 0.0)
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    ) {
        Compass(
            rotation = mapRotation,
            autoHide = false
        ) {
            mapViewModel.setViewpointRotation(0.0)
        }
    }
    mapViewModel.setViewpoint(Viewpoint(39.8, -98.6, 10e7))
}
