package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TC3TwoScreens() {
    // Create a nav controller and host to navigate between ChooseMapScreen and DisplayMapScreen
    // when the user selects a map.
    val navController = rememberNavController()
    val viewModel: TC3ViewModel = viewModel()
    NavHost(navController = navController, startDestination = "chooseMap") {
        composable("chooseMap") {
            ChooseMapScreen(viewModel, navController)
        }
        composable("displayMap") {
            DisplayMapScreen(viewModel, navController)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseMapScreen(viewModel: TC3ViewModel, navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose a map") }) }
    ) {
        val isLoading = viewModel.isLoadingMaps.collectAsState().value
        if (!isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                items(viewModel.maps.size) {
                    ChooseMapRow(map = viewModel.maps[it]) {
                        viewModel.selectMap(it)
                        navController.navigate("displayMap")
                    }
                    Divider()
                }
            }
        } else {
            Text("Loading maps...")
        }
    }
}

@Composable
fun ChooseMapRow(map: ArcGISMap, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = map.getName(), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            "Initial Viewpoint: ${map.getInitialViewpointCenter()}",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayMapScreen(viewModel: TC3ViewModel, navController: NavController) {
    val map = viewModel.selectedMap.collectAsState().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { map.getName() },
                navigationIcon = {
                    // Navigate back to the ChooseMapScreen when the user clicks the back button
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            arcGISMap = map
        )
    }
}

class TC3ViewModel : ViewModel() {
    val maps = listOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(
                Point(
                    -117.9190,
                    33.8121,
                    SpatialReference.wgs84()
                ), 170000.0
            )
        },
        ArcGISMap(BasemapStyle.OsmBlueprint).apply {
            initialViewpoint = Viewpoint(
                53.321736,
                22.697703,
                170000.0
            )
        },
    ).onEach {
        viewModelScope.launch {
            it.load()
            _isLoadingMaps.emit(false)
        }
    }

    private val _isLoadingMaps = MutableStateFlow(true)
    public val isLoadingMaps = _isLoadingMaps.asStateFlow()

    private val _selectedMap = MutableStateFlow<ArcGISMap>(maps[0])
    public val selectedMap = _selectedMap.asStateFlow()

    public fun selectMap(index: Int) {
        _selectedMap.value = maps[index]
    }
}
fun ArcGISMap.getName(): String {
    // If the item.name is not null or empty, return that
    // Else if the basemap.value.name is not null or empty, return that
    // Else return "Unnamed map"
    return if (item?.name == null || item?.name?.isEmpty() == true) {
        if (basemap.value?.name == null || basemap.value?.name?.isEmpty() == true) {
            "Unnamed map"
        } else {
            basemap.value!!.name
        }
    } else {
        item!!.name
    }
}

fun ArcGISMap.getInitialViewpointCenter(): String {
    return if (initialViewpoint == null) {
        "Unknown"
    } else {
        val center = initialViewpoint!!.targetGeometry.extent.center
        val scale = initialViewpoint!!.targetScale
        "Lat: ${center.y.roundToInt()}, Lon: ${center.x.roundToInt()}, Scale: $scale"
    }
}