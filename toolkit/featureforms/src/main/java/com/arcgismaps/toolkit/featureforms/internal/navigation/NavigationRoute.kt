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

import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.screens.FeatureFormScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsFilterScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsScreen
import kotlinx.serialization.Serializable

/**
 * Navigation routes for the form.
 */
@Serializable
internal sealed class NavigationRoute {

    /**
     * Represents the [FeatureFormScreen].
     */
    @Serializable
    data object FormView : NavigationRoute()

    /**
     * Represents a view for the [UNAssociationsFilterScreen].
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected filter.
     */
    @Serializable
    data class UNFilterView(
        val stateId: Int
    ) : NavigationRoute()

    /**
     * Represents a view for the [UNAssociationsScreen].
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected group of associations.
     */
    @Serializable
    data class UNAssociationsView(
        val stateId: Int
    ) : NavigationRoute()

    /**
     * Represents a view for the details of a specific association.
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected association.
     */
    @Serializable
    data class UNAssociationDetailView(
        val stateId: Int
    ) : NavigationRoute()
}
