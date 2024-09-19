/*
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
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.TraceState
import kotlinx.coroutines.launch

/**
 * A composable UI component to set up the navigation for the trace workflow.
 *
 * @param navController The navigation controller to use for navigation
 * @param traceState The state of the trace workflow
 * @since 200.6.0
 */
@Composable
internal fun TraceNavHost(navController: NavHostController, traceState: TraceState) {
    val coroutineScope = rememberCoroutineScope()
    val traceResultAvailable = remember { mutableStateOf(false) }
    if (traceResultAvailable.value) {
        navController.navigate(TraceNavRoute.TraceResults.name)
    }

    NavHost(navController = navController, startDestination = TraceNavRoute.TraceOptions.name) {
        composable(TraceNavRoute.TraceOptions.name) {
            val configs = traceState.traceConfigurations.collectAsStateWithLifecycle()
            TraceOptionsScreen(
                configurations = configs.value,
                startingPoints = traceState.startingPoints,
                onPerformTraceButtonClicked = {
                    coroutineScope.launch {
                        try {
                            traceResultAvailable.value = traceState.trace()
                        } catch (e: Exception) {
                            // Handle error
                            println("ERROR: running traceState.trace() threw an exception: $e")
                        }
                    }
                },
                onAddStartingPointButtonClicked = {
                    traceState.updateAddStartPointMode(AddStartingPointMode.Started)
                    navController.navigate(TraceNavRoute.AddStartingPoint.name)
                },
                selectedConfig = traceState.selectedTraceConfiguration.value,
                onStartingPointRemoved = { traceState.removeStartingPoint(it) },
                onConfigSelected = { newConfig ->
                    traceState.setSelectedTraceConfiguration(newConfig)
                }
            )
        }
        composable(TraceNavRoute.AddStartingPoint.name) {
            AddStartingPointScreen(
                traceState,
                onStopPointSelection = {
                    navController.navigate(TraceNavRoute.TraceOptions.name)
                }
            )
        }
        composable(TraceNavRoute.TraceResults.name) {
            TraceResultScreen(
                traceRun = traceState.currentTraceRun,
                onDeleteResult = {

                }, onZoomToResults = {

                }, onClearAllResults = {

                })
        }
    }
}

/**
 * Defines a navigation route for the trace tool screens.
 *
 * @since 200.6.0
 */
private enum class TraceNavRoute {
    TraceOptions,
    AddStartingPoint,
    TraceResults
    //TODO: Add FeatureAttributes route
}
