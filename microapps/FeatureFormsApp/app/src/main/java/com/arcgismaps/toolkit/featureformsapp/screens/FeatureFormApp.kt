package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.browse.getListOfMaps
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFormApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val mapList = rememberSaveable { getListOfMaps(context) }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Maps",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            navigationIcon = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                if (navBackStackEntry?.destination?.route != "home") {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            }
        )
    }) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    MapListScreen(mapList) { uri ->
                        val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                        val route = "mapview/$encodedUri"
                        navController.popUpTo(route)
                    }
                }
                composable(route = "mapview/{uri}") { backStackEntry ->
                    val uri = backStackEntry.arguments?.getString("uri") ?: ""
                    MapScreen(uri)
                }
            }
        }
    }
}

fun NavController.popUpTo(destination: String) = navigate(destination) {
    popUpTo(graph.findStartDestination().id) {
        saveState = true
    }
    // Restore state when reselecting a previously selected item
    restoreState = true
}
