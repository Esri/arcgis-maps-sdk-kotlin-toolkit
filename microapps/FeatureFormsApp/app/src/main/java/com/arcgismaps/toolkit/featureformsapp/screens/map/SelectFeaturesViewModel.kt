/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.data.ArcGISFeature

class SelectFeaturesViewModel(
    state: UIState.SelectFeature,
) : ViewModel() {

    /**
     * A mapping of layer names to a list of features for that layer.
     */
    val availableFeatures = state.features

    /**
     * The total number of features available for selection.
     */
    val featureCount = state.featureCount

    /**
     * The currently selected features. This list is observable and can be modified by adding or
     * removing features via the [addFeature] and [removeFeature] methods.
     */
    val selectedFeatures: List<ArcGISFeature>
        field: SnapshotStateList<ArcGISFeature> = SnapshotStateList<ArcGISFeature>()

    /**
     * Adds a feature to the list of selected features if it is not already present.
     */
    fun addFeature(feature: ArcGISFeature) {
        if (!selectedFeatures.contains(feature)) {
            selectedFeatures.add(feature)
        }
    }

    /**
     * Removes a feature from the list of selected features if it is present.
     */
    fun removeFeature(feature: ArcGISFeature) {
        selectedFeatures.remove(feature)
    }

    companion object {

        /**
         * Creates a [ViewModelProvider.Factory] for creating instances of [SelectFeaturesViewModel]
         * with the provided [UIState.SelectFeature].
         */
        fun factory(state: UIState.SelectFeature): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SelectFeaturesViewModel(state)
            }
        }
    }
}


