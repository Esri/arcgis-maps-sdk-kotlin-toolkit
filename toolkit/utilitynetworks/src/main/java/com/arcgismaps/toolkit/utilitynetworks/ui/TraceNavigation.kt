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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.TraceState

/**
 * A composable UI component to set up the navigation for the trace workflow.
 *
 * @param navController The navigation controller to use for navigation
 * @param traceState The state of the trace workflow
 * @since 200.6.0
 */
@Composable
internal fun TraceNavHost(navController: NavHostController, traceState: TraceState) {
    NavHost(navController = navController, startDestination = TraceNavRoute.TraceOptions.name) {
        composable(TraceNavRoute.TraceOptions.name) {
            TraceOptionsScreen(
                configurations = emptyList(),
                onPerformTraceButtonClicked = {
                    // TODO: Add call to perform trace
                    navController.navigate(TraceNavRoute.TraceResults.name)
                },
                onAddStartingPointButtonClicked = {
                    traceState.updateAddStartPointMode(AddStartingPointMode.Started)
                    navController.navigate(TraceNavRoute.AddStartingPoint.name)
                })
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
            // TODO: Add TraceResults composable
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
