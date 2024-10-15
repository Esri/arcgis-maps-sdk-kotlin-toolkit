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
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.TraceNavRoute
import com.arcgismaps.toolkit.utilitynetworks.TraceState
import kotlinx.coroutines.launch

/**
 * A composable UI component to set up the navigation for the trace workflow.
 *
 * @param traceState The state of the trace workflow
 * @since 200.6.0
 */
@Composable
internal fun TraceNavHost(traceState: TraceState) {
    val navController = rememberNavController()
    val currentScreen by traceState.currentScreen

    NavHost(navController = navController, startDestination = TraceNavRoute.TraceOptions.name) {
        composable(TraceNavRoute.TraceOptions.name) {
            val configs by traceState.traceConfigurations
            val coroutineScope = rememberCoroutineScope()
            TraceOptionsScreen(
                configurations = configs,
                startingPoints = traceState.currentTraceStartingPoints,
                defaultTraceName = traceState.currentTraceName.value,
                selectedColor = traceState.currentTraceGraphicsColorAsComposeColor,
                zoomToResult = traceState.currentTraceZoomToResults.value,
                showResultsTab = traceState.completedTraces.isNotEmpty(),
                onPerformTraceButtonClicked = {
                    coroutineScope.launch {
                        traceState.trace().onSuccess {
                            traceState.showScreen(TraceNavRoute.TraceResults)
                        }.onFailure {
                            traceState.setCurrentError(it)
                            traceState.showScreen(TraceNavRoute.TraceError)
                        }
                    }
                },
                onAddStartingPointButtonClicked = {
                    traceState.updateAddStartPointMode(AddStartingPointMode.Started)
                    traceState.showScreen(TraceNavRoute.AddStartingPoint)
                },
                selectedConfig = traceState.selectedTraceConfiguration.value,
                onStartingPointRemoved = { traceState.removeStartingPoint(it) },
                onStartingPointSelected = {
                    traceState.setSelectedStartingPoint(it)
                    traceState.showScreen(TraceNavRoute.StartingPointDetails)
                },
                onBackToResults = { traceState.showScreen(TraceNavRoute.TraceResults) },
                onConfigSelected = { newConfig ->
                    traceState.setSelectedTraceConfiguration(newConfig)
                },
                onNameChange = {
                    traceState.setTraceName(it)
                },
                onColorChanged = {
                    traceState.setGraphicsColor(it)
                },
                onZoomRequested = {
                    traceState.setZoomToResults(it)
                }
            )
        }
        composable(TraceNavRoute.AddStartingPoint.name) {
            AddStartingPointScreen(
                onStopPointSelection = {
                    traceState.showScreen(TraceNavRoute.TraceOptions)
                    traceState.updateAddStartPointMode(AddStartingPointMode.Stopped)
                }
            )
        }
        composable(TraceNavRoute.TraceResults.name) {
            TraceResultScreen(
                selectedTraceRunIndex = traceState.selectedCompletedTraceIndex.value,
                traceResults = traceState.completedTraces,
                onSelectPreviousTraceResult = { traceState.selectPreviousCompletedTrace() },
                onSelectNextTraceResult = { traceState.selectNextCompletedTrace()  },
                onBackToNewTrace = { traceState.showScreen(TraceNavRoute.TraceOptions) },
                onFeatureGroupSelected = {
                    traceState.setGroupName(it)
                    traceState.showScreen(TraceNavRoute.FeatureResultsDetails)
                },
                onDeleteResult = {

                }, onZoomToResults = {

                }, onClearAllResults = {

                }
            )
        }
        composable(TraceNavRoute.FeatureResultsDetails.name) {
            val coroutineScope = rememberCoroutineScope()
            val selectedGroupName = traceState.selectedGroupName
            require(selectedGroupName != null)
            FeatureResultsDetailsScreen(
                selectedGroupName = selectedGroupName,
                elementListWithSelectedGroupName = traceState.getAllElementsWithSelectedGroupName(),
                onBackToResults = { traceState.showScreen(TraceNavRoute.TraceResults) },
                onBackToNewTrace = { traceState.showScreen(TraceNavRoute.TraceOptions) },
                onFeatureSelected = {
                    coroutineScope.launch {
                        traceState.zoomToUtilityElement(it)
                    }
                }
            )
        }
        dialog(TraceNavRoute.TraceError.name) {
            TraceErrorDialog(
                error = traceState.currentError ?: return@dialog,
                onConfirmation = {
                    traceState.showScreen(TraceNavRoute.TraceOptions)
                }
            )
        }
        composable(TraceNavRoute.StartingPointDetails.name) {
            val coroutineScope = rememberCoroutineScope()
            val startingPoint = traceState.selectedStartingPoint.value
            require(startingPoint != null)
            StartingPointDetailsScreen(
                startingPoint,
                showResultsTab = traceState.completedTraces.isNotEmpty(),
                onBackToResults = { traceState.showScreen(TraceNavRoute.TraceResults) },
                onZoomTo = {
                    coroutineScope.launch {
                        traceState.zoomToStartingPoint(startingPoint)
                    }
                }, onDelete = {
                    traceState.removeStartingPoint(startingPoint)
                    traceState.showScreen(TraceNavRoute.TraceOptions)
                },
                onFractionChanged = { point, newValue ->
                    traceState.setFractionAlongEdge(
                        point,
                        newValue.toDouble()
                    )
                },
                onTerminalSelected = { utilityTerminal ->
                    traceState.setTerminal(
                        startingPoint,
                        utilityTerminal
                    )
                },
                onBackPressed = { traceState.showScreen(TraceNavRoute.TraceOptions) })
        }
    }

    if (navController.currentDestination?.route != currentScreen.name) {
        navController.navigate(currentScreen.name)
    }
}
