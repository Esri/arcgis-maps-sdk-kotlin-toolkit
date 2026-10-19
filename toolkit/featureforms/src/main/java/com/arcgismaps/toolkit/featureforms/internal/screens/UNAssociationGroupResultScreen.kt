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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationGroupResult
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState

/**
 * Screen that displays the selected group of associations.
 *
 * @param state The state of the utility association element.
 * @param featureForm The feature form associated with the current feature.
 * @param isNavigationEnabled Flag indicating whether navigation to associated features is enabled.
 * @param onNavigateToAssociation The callback to be invoked when the user selects to view association details.
 * @param onNavigateToFeature The callback to be invoked when the user selects a feature to navigate to.
 * @param onAssociatedFeatureLocateRequest The callback to be invoked when a locate request is made for an associated feature.
 * @param onBack The callback to invoke when the back action is triggered.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationGroupResultScreen(
    state: UtilityAssociationsElementState,
    featureForm: FeatureForm,
    isNavigationEnabled: Boolean,
    onNavigateToAssociation: () -> Unit,
    onNavigateToFeature: (ArcGISFeature) -> Unit,
    onAssociatedFeatureLocateRequest: (ArcGISFeature) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupResult = state.selectedGroupResult ?: return
    val isEditable by state.isEditable.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        // Display the top bar
        FeatureFormTitle(
            title = groupResult.name,
            subTitle = state.selectedFilterResult?.filter?.title ?: "",
            hasEdits = false,
            showCloseIcon = false,
            showBackIcon = false,
            isNavigationEnabled = true,
            onBackPressed = {},
            onClose = {},
            onSave = {},
            onDiscard = {},
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )

        UtilityAssociationGroupResult(
            groupResult = groupResult,
            isEditable = isEditable,
            isNavigationEnabled = isNavigationEnabled,
            onAssociatedFeatureLocateRequest = onAssociatedFeatureLocateRequest,
            onItemClick = { index ->
                val feature = groupResult.associationResults[index].associatedFeature
                // Navigate to the next form if there are no edits.
                onNavigateToFeature(feature)
            },
            onDetailsClick = { index ->
                val association = groupResult.associationResults[index]
                state.setSelectedAssociationResult(association)
                // show the details sheet
                // showDetails = true
                onNavigateToAssociation()
            },
            onDelete = { isGroupEmpty ->
                if (isGroupEmpty) {
                    // If the group is empty after deletion, navigate back to the filter view
                    onBack()
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
