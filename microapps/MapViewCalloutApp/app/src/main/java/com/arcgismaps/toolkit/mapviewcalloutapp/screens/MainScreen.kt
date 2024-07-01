/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.geoviewcompose.MapView

private val calloutAppScreens = mutableListOf(
    "Show Callout on a tap location",
    "Show Callout on a Feature",
    "Show Callout on a Graphic"
)

/**
 * Displays a list of screens to launch. Each of which demonstrates different ways to show
 * a Callout on a [MapView].
 */
@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf("") }
    val navController = rememberNavController()

    navController.addOnDestinationChangedListener(listener = { _, destination, _ ->
        currentScreen = destination.route.toString()
    })

    CalloutAppNavHost(
        navController = navController,
        calloutScreenNames = calloutAppScreens,
        currentScreen = currentScreen
    ) {
        composable(route = calloutAppScreens[0]) {
            val tapLocationViewModel: MapViewModel = viewModel()
            TapLocationScreen(tapLocationViewModel)
        }
        composable(route = calloutAppScreens[1]) {
            val featureViewModel: MapViewModel = viewModel()
            FeatureScreen(featureViewModel)
        }
        composable(route = calloutAppScreens[2]) {
            val graphicsViewModel : MapViewModel = viewModel()
            GraphicScreen(graphicsViewModel)
        }
    }
}


@Composable
fun CalloutAppNavHost(
    navController: NavHostController,
    calloutScreenNames: MutableList<String>,
    currentScreen: String,
    builder: NavGraphBuilder.() -> Unit
) {
    Scaffold(
        topBar = {
            CalloutAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = "Callout App",
        ) {
            composable(route = "Callout App") {
                NavScreenSwitcher(
                    calloutScreenNames = calloutScreenNames,
                    onScreenSelected = { selectedScreen ->
                        navController.navigate(selectedScreen)
                    }
                )
            }
            builder.invoke(this)
        }
    }
}

@Composable
fun NavScreenSwitcher(
    calloutScreenNames: List<String>,
    onScreenSelected: (String) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(24.dp),
                text = "Select screen to launch"
            )

            val (selectedOption, onOptionSelected) = remember { mutableStateOf(calloutScreenNames[0]) }
            Column(
                Modifier
                    .selectableGroup()
                    .padding(24.dp)
            ) {
                calloutScreenNames.forEach { calloutScreen ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (calloutScreen == selectedOption),
                                onClick = { onOptionSelected(calloutScreen) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (calloutScreen == selectedOption),
                            onClick = null
                        )
                        Text(
                            text = calloutScreen,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Button(onClick = { onScreenSelected(selectedOption) }) {
                Text(text = "Launch screen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalloutAppBar(
    currentScreen: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(currentScreen) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back button"
                    )
                }
            }
        }
    )
}
