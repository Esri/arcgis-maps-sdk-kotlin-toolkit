package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geocompose.ViewpointPersistence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC1_7_InitialViewpoint() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC1.7: Single MapView with Initial Viewpoint") })
        }
    ) {
        var viewpointPersistence: ViewpointPersistence by rememberSaveable { mutableStateOf(
            ViewpointPersistence.ByCenterAndScale()) }

        MapViewWithConfigurablePersistence(
            viewpointPersistence = viewpointPersistence,
            onViewpointPersistenceSelected = { viewpointPersistence = it },
            modifier = Modifier.padding(it),
            arcGISMap = remember {
                ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                    initialViewpoint = Viewpoint(43.5, 13.5, 20000000.0)
                }
            }
        )
    }
}