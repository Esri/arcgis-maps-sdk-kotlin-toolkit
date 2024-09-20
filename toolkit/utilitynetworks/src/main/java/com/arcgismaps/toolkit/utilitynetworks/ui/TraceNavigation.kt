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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.TraceNavRoute
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
internal fun TraceNavHost(traceState: TraceState) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by traceState.currentScreen

    NavHost(navController = navController, startDestination = TraceNavRoute.TraceOptions.name) {
        composable(TraceNavRoute.TraceOptions.name) {
            val configs by traceState.traceConfigurations
            TraceOptionsScreen(
                configurations = configs,
                startingPoints = traceState.startingPoints,
                onPerformTraceButtonClicked = {
                    coroutineScope.launch {
                        try {
                            if (traceState.trace()) {
                                traceState.showScreen(TraceNavRoute.TraceResults)
                            }
                        } catch (e: Exception) {
                            // Handle error
                            println("ERROR: running traceState.trace() threw an exception: $e")
                        }
                    }
                },
                onAddStartingPointButtonClicked = {
                    traceState.updateAddStartPointMode(AddStartingPointMode.Started)
                    traceState.showScreen(TraceNavRoute.AddStartingPoint)
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
                    traceState.showScreen(TraceNavRoute.TraceOptions)
                    traceState.updateAddStartPointMode(AddStartingPointMode.Stopped)
                }
            )
        }
        composable(TraceNavRoute.TraceResults.name) {
            TraceResultScreen(
                traceRun = traceState.currentTraceRun,
                onDeleteResult = {

                }, onZoomToResults = {

                }, onClearAllResults = {

                },
                onDismiss = {
                    traceState.showScreen(TraceNavRoute.TraceOptions)
                }
            )
        }
    }

    navController.navigate(currentScreen.name)
}
