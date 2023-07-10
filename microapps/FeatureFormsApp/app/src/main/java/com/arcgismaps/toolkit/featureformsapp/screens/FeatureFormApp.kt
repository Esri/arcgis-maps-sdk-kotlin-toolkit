package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.featureformsapp.data.ItemDataSource
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun FeatureFormApp() {
    // create a NavController
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val portalItemUseCase = PortalItemUseCase(ItemRepository(ItemDataSource(IO), coroutineScope), coroutineScope)
    // create a NavHost with a navigation graph builder
    NavHost(navController = navController, startDestination = "home") {
        // Home screen - shows the list of maps
        composable("home") {
            MapListScreen { uri ->
                // encode the uri since it is equivalent to a navigation route
                val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                val route = "mapview/$encodedUri"
                // navigate to the mapview
                navController.navigate(route)
            }
        }
        // MapView Screen - shows the map and the FeatureForms
        composable(route = "mapview/{uri}") { backStackEntry ->
            // fetch the uri from the route arguments
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            MapScreen(uri) {
                // navigate back on back pressed
                navController.navigateUp()
            }
        }
    }
}
