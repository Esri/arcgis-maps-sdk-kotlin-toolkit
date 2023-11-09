/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.login.LoginScreen
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Navigator {
    private val _navigationFlow = MutableSharedFlow<NavigationRoute>(extraBufferCapacity = 1)
    val navigationFlow = _navigationFlow.asSharedFlow()

    fun navigateTo(route: NavigationRoute) {
        _navigationFlow.tryEmit(route)
    }
}

sealed class NavigationRoute private constructor(val route: String) {
    object Login : NavigationRoute("login")
    object Home : NavigationRoute("home")
    object MapView : NavigationRoute("mapview/{uri}")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    navigator: Navigator,
    startDestination : String,
) {
    LaunchedEffect(Unit) {
        navigator.navigationFlow.collect {
            navController.navigate(it.route) {
                if (it == NavigationRoute.Login) {
                    navController.popBackStack()
                }
            }
        }
    }
    // create a NavHost with a navigation graph builder
    NavHost(navController = navController, startDestination = startDestination) {
        // Login screen
        composable(NavigationRoute.Login.route) {
            LoginScreen {
                // on successful login, go to the map list screen
                navController.navigate(NavigationRoute.Home.route) {
                    // remove this entry from the nav stack to disable a "back" action
                    navController.popBackStack()
                }
            }
        }
        // Home screen - shows the list of maps
        composable(NavigationRoute.Home.route) {
            MapListScreen { uri ->
                // encode the uri since it is equivalent to a navigation route
                val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                val route = "mapview/$encodedUri"
                // navigate to the mapview
                navController.navigate(route)
            }
        }
        // MapView Screen - shows the map and the FeatureForms
        composable(NavigationRoute.MapView.route) {
            MapScreen {
                // navigate back on back pressed
                navController.navigateUp()
            }
        }
    }
}
