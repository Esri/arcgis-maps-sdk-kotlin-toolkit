package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geoviewcompose.MapView

// Case b.
//- Show a MapView with a map with a Feature layer with features
//- Display callout using Point(tap location) on a feature (with some text)
//- Display a graphic at the tapped location
//- add switch to enable/disable animation

@Composable
fun AppScreen2(){
    Box(modifier = Modifier.fillMaxSize()){
        MapView(
            arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)
        )
    }
}