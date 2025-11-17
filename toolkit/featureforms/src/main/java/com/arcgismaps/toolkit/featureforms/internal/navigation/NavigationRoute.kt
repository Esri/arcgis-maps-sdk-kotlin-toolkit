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
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationGroupResultScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsFilterResultScreen
import kotlinx.serialization.Serializable

/**
 * Navigation routes for the form.
 */
@Serializable
internal sealed class NavigationRoute {

    /**
     * Represents a route for the [FeatureFormScreen].
     */
    @Serializable
    data object Form : NavigationRoute()

    /**
     * Represents a route for the [UNAssociationsFilterResultScreen].
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected filter.
     */
    @Serializable
    data class UNAssociationsFilterResult(
        val stateId: Int
    ) : NavigationRoute()

    /**
     * Represents a route for the [UNAssociationGroupResultScreen].
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected group of associations.
     */
    @Serializable
    data class UNAssociationGroupResult(
        val stateId: Int
    ) : NavigationRoute()

    /**
     * Represents a route for adding an association from a source feature. This is represented as
     * a nested navigation graph. See [AddFromSourceNavRoute] for the routes in this nested graph.
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected association to add from source.
     */
    @Serializable
    data class AddUNAssociationFromSource(
        val stateId: Int
    ) : NavigationRoute()

    /**
     * Represents a route for navigating to a specific association's details.
     *
     * @param stateId The state ID of the [UtilityAssociationsElementState] which contains the
     * selected association.
     */
    @Serializable
    data class UNAssociationDetails(
        val stateId: Int
    ) : NavigationRoute()
}

/**
 * Navigation routes for the "Add from Source" nested navigation graph.
 */
@Serializable
internal sealed class AddFromSourceNavRoute {

    /**
     * Represents the screen to select a source.
     */
    @Serializable
    data object SelectSource : AddFromSourceNavRoute()

    /**
     * Represents the screen to select an asset group.
     */
    @Serializable
    data object SelectAssetType : AddFromSourceNavRoute()

    /**
     * Represents the screen to select a feature from the source.
     */
    @Serializable
    data object SelectAssociatedFeature : AddFromSourceNavRoute()

    /**
     * Represents the review screen before adding the association.
     */
    @Serializable
    data object CreateAssociation : AddFromSourceNavRoute()
}
