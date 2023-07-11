package com.arcgismaps.toolkit.floorfilterapp.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.indoors.FloorFilter

@Composable
fun MainScreen() {
    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "f133a698536f44c8884ad81f80b6cfc7")
    val floorAwareWebMap = ArcGISMap(portalItem)

    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(floorAwareWebMap))

    val floorFilterViewModel = viewModel<FloorFilterViewModel>(
        factory = FloorFilterViewModelFactory("Hello Template!!")
    )

    ComposableMap(
        modifier = Modifier.fillMaxSize(),
        mapInterface = mapViewModel
    ) {
        Row(modifier = Modifier.wrapContentSize().padding(25.dp)) {
            // TBD: Need to work if we should get an instance of the GeoView/Map/Scene
            FloorFilter(arcGISMap = floorAwareWebMap)

        }
    }
}
