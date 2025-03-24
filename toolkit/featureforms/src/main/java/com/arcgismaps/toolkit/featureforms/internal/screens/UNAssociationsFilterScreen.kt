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

package com.arcgismaps.toolkit.featureforms.internal.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog

/**
 * Screen that displays the selected filter for the utility associations.
 *
 * @param formStateData The form state data.
 * @param route The [NavigationRoute.UNFilterView] route data of this screen.
 * @param onGroupSelected The callback that is invoked when a group is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsFilterScreen(
    formStateData: FormStateData,
    route : NavigationRoute.UNFilterView,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val states = formStateData.stateCollection
    // Get the selected UtilityAssociationsElementState from the state collection
    val utilityAssociationsElementState = states[route.stateId]
        // guard against null value
        as? UtilityAssociationsElementState ?: return
    // Get the selected filter from the UtilityAssociationsElementState
    val filterResult = utilityAssociationsElementState.selectedFilterResult
    // guard against null value
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        if (filterResult != null) {
            UtilityAssociationFilter(
                groupResults = filterResult.groupResults,
                onGroupClick = { groupResult ->
                    utilityAssociationsElementState.setSelectedGroupResult(groupResult)
                    onGroupSelected(utilityAssociationsElementState.id)
                },
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
            )
        }
    }
    FeatureFormDialog(states)
}
