package com.arcgismaps.toolkit.authenticationapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapFlow

@Composable
fun MainScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )
    mapViewModel.setViewpoint(
        viewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        ),
        channel = MapFlow.Channel.Write
    )
}
