package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets

@Composable
fun MainScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic).also {
        it.initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }
    
    val mapViewModel = viewModel<FeatureFormsMapViewModel>(
        factory = MapViewModelFactory(
            map,
            MapInsets(bottom = 25.0)
        )
    )
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    )
}
