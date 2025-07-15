/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "SelectionScreen") {
        composable("SelectionScreen") { SelectionScreen(navController) }
        composable("TC1_SingleMapView") { TC1SingleMapView() }
        composable("TC1_7_InitialViewpoint") { TC1_7_InitialViewpoint() }
        composable("TC2_TwoMapViews") { TC2TwoMapViews() }
        composable("TC3_TwoScreens") { TC3TwoScreens() }
        composable("TC4_SceneView") { TC4SceneView() }
        composable("TC5_AnimatedViewpoint") { TC5AnimatedViewpoint() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Viewpoint Persistence Tests", style = MaterialTheme.typography.headlineMedium)
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            item {
                ScreenSelectionItem(text = "TC 1: Single MapView", onClick = {
                    navController.navigate("TC1_SingleMapView")
                })
            }
            item {
                ScreenSelectionItem(
                    text = "TC 1.7: Single MapView with Initial Viewpoint",
                    onClick = {
                        navController.navigate("TC1_7_InitialViewpoint")
                    })
            }
            item {
                ScreenSelectionItem(text = "TC 2: Two MapViews", onClick = {
                    navController.navigate("TC2_TwoMapViews")
                })
            }
            item {
                ScreenSelectionItem(text = "TC 3: Two Screens") {
                    navController.navigate("TC3_TwoScreens")
                }
            }
            item {
                ScreenSelectionItem(text = "TC 4: Single SceneView") {
                    navController.navigate("TC4_SceneView")
                }
            }
            item {
                ScreenSelectionItem(text = "TC 5: Animated Viewpoint") {
                    navController.navigate("TC5_AnimatedViewpoint")
                }
            }
        }
    }
}

@Composable
fun ScreenSelectionItem(text: String, onClick: () -> Unit) {
    Text(text = text, modifier = Modifier
        .padding(16.dp)
        .clickable { onClick() }, style = MaterialTheme.typography.headlineMedium)
    Divider()
}
