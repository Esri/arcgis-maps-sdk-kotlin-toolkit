package com.arcgismaps.toolkit.authenticationapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.Duplex
import com.arcgismaps.toolkit.composablemap.ComposableMap

@Composable
fun MainScreen(portalItem: PortalItem) {
    val map = ArcGISMap(portalItem)
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
        duplex = Duplex.Write
    )
}
