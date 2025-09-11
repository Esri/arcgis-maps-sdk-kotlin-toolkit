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

package com.arcgismaps.toolkit.popup.internal.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.popup.PopupStateData
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociations
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationRoute

/**
 * Screen that displays the selected group of associations.
 *
 * @param popupStateData The popup state data.
 * @param route The [NavigationRoute.UNAssociationsView] route data of this screen.
 * @param onNavigateToFeature The callback to be invoked when the user selects a feature to navigate to.
 * @param onNavigateToAssociation The callback to be invoked when the user selects an association to navigate to.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsScreen(
    popupStateData: PopupStateData,
    route: NavigationRoute.UNAssociationsView,
    onNavigateToFeature: (ArcGISFeature) -> Unit,
    onNavigateToAssociation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val states = popupStateData.stateCollection
    // Get the selected UtilityAssociationsElementState from the state collection
    val utilityAssociationsElementState = states[route.stateId] as?
        UtilityAssociationsElementState ?: return
    // Get the selected filter from the UtilityAssociationsElementState
    val filterResult = utilityAssociationsElementState.selectedFilterResult
    // Get the selected group from the filter
    val groupResult = utilityAssociationsElementState.selectedGroupResult
    if (filterResult == null || groupResult == null) {
        // guard against null values
        return
    }
    UtilityAssociations(
        groupResult = groupResult,
        isNavigationEnabled = true,
        onItemClick = { index ->
            val feature = groupResult.associationResults[index].associatedFeature
            // Navigate to the next popup if there are no edits.
            onNavigateToFeature(feature)
        },
        onDetailsClick = { index ->
            val association = groupResult.associationResults[index]
            utilityAssociationsElementState.setSelectedAssociationResult(association)
            onNavigateToAssociation(utilityAssociationsElementState.id)
        },
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    )
}
