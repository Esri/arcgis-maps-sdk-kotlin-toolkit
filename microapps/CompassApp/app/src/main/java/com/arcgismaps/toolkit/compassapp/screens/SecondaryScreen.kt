package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.compassapp.navigation.CompassScreens
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.DuplexFlow

/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

@Composable
fun SecondaryScreen(navController: NavController) {
    // create an ArcGISMap with a Topographic basemap style
    val map = ArcGISMap(BasemapStyle.ArcGISImagery)
    // instantiate a MapViewModel using the factory
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(map))
    // hoist the mapRotation state
    val mapRotation by mapViewModel.mapRotation.collectAsState(DuplexFlow.Type.Read)
//     show a composable map using the mapViewModel

    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    ) {
        Button(onClick = {
            //your onclick code here
            navController.navigate(route = CompassScreens.MainScreen.name)
        }) {
            Text(text = "Switch MapView")
        }
        Row(modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .padding(25.dp)) {
            // show the compass and pass the mapRotation state data
            Compass(rotation = mapRotation) {
                // reset the ComposableMap viewpoint rotation to point north using the mapViewModel
                mapViewModel.setViewpointRotation(0.0)
            }
        }
    }
    // set the composable map's viewpoint to North America
    mapViewModel.setViewpoint(Viewpoint(39.8, -98.6, 10e7))

}
