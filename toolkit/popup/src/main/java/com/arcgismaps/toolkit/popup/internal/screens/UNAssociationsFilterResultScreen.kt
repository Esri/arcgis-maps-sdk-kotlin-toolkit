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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.popup.UtilityAssociationsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsFilterResult
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationRoute
import com.arcgismaps.toolkit.popup.PopupStateData

/**
 * Screen that displays the selected filter for a [UtilityAssociationsPopupElement].
 *
 * @param popupStateData The popup state data.
 * @param route The [NavigationRoute.UNFilterView] route data of this screen.
 * @param onGroupSelected The callback that is invoked when a group is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsFilterResultScreen(
    popupStateData: PopupStateData,
    route : NavigationRoute.UNFilterView,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val states = popupStateData.stateCollection
    // Get the selected UtilityAssociationsElementState from the state collection
    val utilityAssociationsElementState = states[route.stateId]
        // guard against null value
        as? UtilityAssociationsElementState ?: return
    // Get the selected filter from the UtilityAssociationsElementState
    val filterResult = utilityAssociationsElementState.selectedFilterResult
    // guard against null value
    if (filterResult != null) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Top
        ) {
            UtilityAssociationsFilterResult(
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
}
