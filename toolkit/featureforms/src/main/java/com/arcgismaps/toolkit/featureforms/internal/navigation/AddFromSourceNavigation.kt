/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.navigation

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.arcgismaps.toolkit.featureforms.FeatureFormState

internal fun NavGraphBuilder.selectSourceDestination(
    onSourceSelected : (NavBackStackEntry) -> Unit,
    state : FeatureFormState
) {
    composable<AddFromSourceNavRoute.SelectSource> { backStackEntry ->
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        val stateId = backStackEntry.arguments?.getInt("stateId") ?: -1
        val state = formData.stateCollection[stateId] ?: return@composable
        Column {
            Text(text = backStackEntry.destination.route.toString())
            Button(
                onClick = {
                    onSourceSelected(backStackEntry)
                }
            ) {
                Text("Go to Select Asset Type for stateId $state")
            }
        }
    }
}

internal fun NavGraphBuilder.selectAssetTypeDestination() {
    composable<AddFromSourceNavRoute.SelectAssetType> { backStackEntry ->
        Log.d(
            "FeatureFormNavHost",
            "In SelectAssetType: ${backStackEntry.destination.route}"
        )
        val parent = backStackEntry.destination.parent?.route
        Log.e("TAG", "FeatureFormNavHost: parent $parent")
        Text(text = backStackEntry.destination.route.toString())
    }
}

internal fun NavHostController.navigateToAddUNAssociationFromSourceView(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.AddUNAssociationFromSourceView(stateId = stateId)
    // Navigate to the add from source view
    navigateSafely(backStackEntry, newRoute)
}

internal fun NavHostController.navigateToSelectAssetType(
    backStackEntry: NavBackStackEntry
) {
    val newRoute = AddFromSourceNavRoute.SelectAssetType
    navigateSafely(backStackEntry, newRoute)
}
