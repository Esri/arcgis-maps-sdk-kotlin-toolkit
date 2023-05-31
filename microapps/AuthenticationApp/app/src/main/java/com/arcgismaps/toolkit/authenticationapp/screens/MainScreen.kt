package com.arcgismaps.toolkit.authenticationapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface

@Composable
fun MainScreen(portalItemUrl: String) {
    val map = ArcGISMap(PortalItem(portalItemUrl))
    val insets = MapInsets(bottom = 25.0)
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map, insets))
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )
    mapViewModel.setViewpoint(Viewpoint(39.8, -98.6, 10e7))
}
