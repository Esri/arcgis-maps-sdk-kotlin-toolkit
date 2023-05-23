package com.arcgismaps.toolkit.templateapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets

@Composable
fun MainScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val viewpoint = Viewpoint(39.8, -98.6, 10e7)
    val insets = MapInsets(bottom = 25.0)
    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = map,
        initialViewPoint = viewpoint,
        insets = insets
    )
}
