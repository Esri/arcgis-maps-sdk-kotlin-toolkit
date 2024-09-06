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
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 *
 * @since 200.6.0
 */
@Composable
internal fun TraceNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = TraceOptions.route) {
        composable(TraceOptions.route) {
            TraceOptions(onPerformTrace = {
                navController.navigate(TraceResults.route)
            },
                onPointSelected = {
                    navController.navigate(AddStartingPoint.route)
                })
        }
        composable(AddStartingPoint.route) {
            StartingPoints(
                onPointSelected = {
                    navController.navigate(TraceOptions.route)
                }
            )
        }
        composable(TraceResults.route) {
            // TODO: Add TraceResults composable
        }

    }
}


internal sealed interface TraceDestination {
    val route: String
}

internal data object TraceOptions : TraceDestination {
    override val route = "trace_options"
}

internal data object TraceResults : TraceDestination {
    override val route = "trace_results"
}

internal data object AddStartingPoint : TraceDestination {
    override val route = "add_starting_point"
}
//TODO: Add FeatureAttributes destination
