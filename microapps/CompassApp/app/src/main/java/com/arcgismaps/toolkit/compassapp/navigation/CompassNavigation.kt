package com.arcgismaps.toolkit.compassapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.toolkit.compassapp.screens.MainScreen
import com.arcgismaps.toolkit.compassapp.screens.SecondaryScreen

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
fun CompassNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController,
        startDestination = CompassScreens.MainScreen.name) {
        composable(CompassScreens.MainScreen.name) {
            MainScreen(navController = navController)
        }
        composable(CompassScreens.MainScreen.name) {
            MainScreen(navController = navController)
        }

//        val route = CompassScreens.MainScreen.name
//        composable("$route/{")

        composable(CompassScreens.SecondaryScreen.name) {
            SecondaryScreen(navController = navController)
        }
    }
}
