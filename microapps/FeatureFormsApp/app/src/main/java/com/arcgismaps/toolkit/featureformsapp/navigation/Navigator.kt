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

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.screens.browse.FolderContentScreen
import com.arcgismaps.toolkit.featureformsapp.screens.browse.FolderContentViewModel
import com.arcgismaps.toolkit.featureformsapp.screens.browse.FolderContentViewModelFactory
import com.arcgismaps.toolkit.featureformsapp.screens.browse.PortalContentScreen
import com.arcgismaps.toolkit.featureformsapp.screens.login.LoginScreen
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import com.arcgismaps.toolkit.featureformsapp.screens.search.SearchScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

class Navigator {
    private val _navigationFlow = MutableSharedFlow<NavigationRoute>(extraBufferCapacity = 1)
    val navigationFlow = _navigationFlow.asSharedFlow()

    fun navigateTo(route: NavigationRoute) {
        _navigationFlow.tryEmit(route)
    }
}

@Serializable
sealed class NavigationRoute {

    @Serializable
    object Login : NavigationRoute()

    @Serializable
    object Home : NavigationRoute()

    @Serializable
    data class Folder(val folder: PortalFolder) : NavigationRoute()

    @Serializable
    object Search : NavigationRoute()

    @Serializable
    object MapView : NavigationRoute()
}

@Composable
@Suppress("DEPRECATION")
fun AppNavigation(
    navController: NavHostController,
    navigator: Navigator,
    startDestination: NavigationRoute
) {
    LaunchedEffect(Unit) {
        navigator.navigationFlow.collect {
            navController.navigate(it) {
                if (it == NavigationRoute.Login) {
                    navController.popBackStack()
                }
            }
        }
    }
    // create a NavHost with a navigation graph builder
    NavHost(navController = navController, startDestination = startDestination) {
        // Login screen
        composable<NavigationRoute.Login>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            LoginScreen {
                // on successful login, go to the map list screen
                navController.navigate(NavigationRoute.Home) {
                    // remove this entry from the nav stack to disable a "back" action
                    navController.popBackStack()
                }
            }
        }
        // Home screen - shows the list of maps
        composable<NavigationRoute.Home>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            PortalContentScreen(
                onFolderClick = {
                    // navigate to the folder screen
                    val route = NavigationRoute.Folder(it)
                    navController.navigate(route)
                },
                onItemSelected = {
                    // navigate to the mapview
                    navController.navigate(NavigationRoute.MapView) {
                        restoreState = true
                    }
                },
                onSearchIconClick = {
                    navController.navigate(NavigationRoute.Search) {
                        restoreState = true
                        launchSingleTop = true
                    }
                }
            )

        }

        composable<NavigationRoute.Folder>(
            typeMap = mapOf(typeOf<PortalFolder>() to PortalFolderNavType),
            enterTransition = { slideInHorizontally { h -> h } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { h -> h } }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.Folder>()
            val viewModel = hiltViewModel<FolderContentViewModel, FolderContentViewModelFactory> {
                it.create(route.folder)
            }
            FolderContentScreen(
                viewModel = viewModel,
                onItemSelected = {
                    // navigate to the mapview
                    navController.navigate(NavigationRoute.MapView)
                },
                onSearchIconClick = {
                    // navigate to the search screen
                    navController.navigate(NavigationRoute.Search) {
                        restoreState = true
                        launchSingleTop = true
                    }
                },
                onBackPressed = {
                    if (navController.previousBackStackEntry == null) {
                        navController.navigate(NavigationRoute.Home) {
                            navController.popBackStack()
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                hasBackStack = navController.previousBackStackEntry != null
            )
        }

        composable<NavigationRoute.Search>(
            enterTransition = { fadeIn() + slideInHorizontally { h -> h } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + slideOutHorizontally { h -> h }}
        ) {
            SearchScreen(
                onItemSelected = {
                    // navigate to the mapview
                    navController.navigate(NavigationRoute.MapView)
                }
            ) {
                navController.navigateUp()
            }
        }

        // MapView Screen - shows the map and the FeatureForms
        composable<NavigationRoute.MapView>(
            enterTransition = { slideInHorizontally { h -> h } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { h -> h } }
        ) {
            MapScreen {
                // navigate back on back pressed
                navController.popBackStack()
            }
        }
    }
}
