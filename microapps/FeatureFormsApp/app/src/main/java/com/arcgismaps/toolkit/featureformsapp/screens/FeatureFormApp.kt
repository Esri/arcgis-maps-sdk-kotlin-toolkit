package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun FeatureFormApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MapListScreen { uri ->
                val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                val route = "mapview/$encodedUri"
                navController.navigate(route)
            }
        }
        composable(route = "mapview/{uri}") { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            MapScreen(uri) {
                navController.navigateUp()
            }
        }
    }
}
