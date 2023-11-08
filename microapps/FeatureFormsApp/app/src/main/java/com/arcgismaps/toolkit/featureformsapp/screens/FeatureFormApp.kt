package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.login.LoginScreen
import com.arcgismaps.toolkit.featureformsapp.screens.login.LoginViewModel
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun FeatureFormApp() {
    // create a NavController
    val navController = rememberNavController()
    // create a NavHost with a navigation graph builder
    NavHost(navController = navController, startDestination = "login") {
        // Login screen
        composable("login") {
            val loginViewModel = hiltViewModel<LoginViewModel>()
            LoginScreen(loginViewModel)
        }
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
        composable(route = "mapview/{uri}") {
            MapScreen {
                // navigate back on back pressed
                navController.navigateUp()
            }
        }
    }
}
