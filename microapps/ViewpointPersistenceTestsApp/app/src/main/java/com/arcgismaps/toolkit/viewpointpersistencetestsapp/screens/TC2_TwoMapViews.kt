package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.layout.Column
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
import com.arcgismaps.toolkit.geoviewcompose.ViewpointPersistence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC2TwoMapViews() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC2: Two MapViews") })
        }
    ) {
        var viewpointPersistenceTop: ViewpointPersistence by rememberSaveable { mutableStateOf(
            ViewpointPersistence.ByCenterAndScale()) }
        var viewpointPersistenceBottom: ViewpointPersistence by rememberSaveable { mutableStateOf(
            ViewpointPersistence.ByBoundingGeometry()) }

        Column(modifier = Modifier.padding(it)) {
            MapViewWithConfigurablePersistence(
                viewpointPersistence = viewpointPersistenceTop,
                onViewpointPersistenceSelected = { viewpointPersistenceTop = it },
                modifier = Modifier.weight(0.5f),
                arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) },
                useSquareAspectRatio = false
            )

            MapViewWithConfigurablePersistence(
                viewpointPersistence = viewpointPersistenceBottom,
                onViewpointPersistenceSelected = { viewpointPersistenceBottom = it },
                modifier = Modifier.weight(0.5f),
                arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
                useSquareAspectRatio = false
            )
        }
    }
}