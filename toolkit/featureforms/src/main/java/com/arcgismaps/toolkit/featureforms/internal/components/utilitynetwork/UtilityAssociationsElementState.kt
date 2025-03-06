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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
internal class UtilityFilterState(
    val filter: UtilityAssociationsFilter,
    val groups: List<UtilityFilterGroupState>,
    val count: Int
)

@Immutable
internal class UtilityFilterGroupState(
    val name: String,
    val associationsInfo: List<AssociationInfoState>
) {
    val count = associationsInfo.size
}

@Immutable
internal class AssociationInfoState(
    val associatedFeature: ArcGISFeature,
    val association: UtilityAssociation
)

internal class UtilityAssociationsElementState(
    element: UtilityAssociationsFormElement,
    scope: CoroutineScope
) : FormElementState(
    id = element.hashCode(),
    label = element.label,
    description = element.description,
    isVisible = element.isVisible
) {
    private var _loading = mutableStateOf(true)

    val loading: State<Boolean>
        get() = _loading

    var filters: MutableState<List<UtilityFilterState>> = mutableStateOf(emptyList())
        private set

    init {
        scope.launch {
            element.fetchAssociationsFilterResults()
            filters.value = element.associationsFilterResults.map { filterResult ->
                val groups = filterResult.groupResults.map { groupResult ->
                    val infos = groupResult.associationResults.map { associationResult ->
                        AssociationInfoState(
                            associationResult.associatedFeature,
                            associationResult.association
                        )
                    }
                    UtilityFilterGroupState(
                        groupResult.name,
                        infos
                    )
                }
                UtilityFilterState(
                    filterResult.filter,
                    groups,
                    filterResult.resultCount
                )
            }
            _loading.value = false
        }
    }
}

internal fun UtilityAssociation.getTargetElement(arcGISFeature: ArcGISFeature): UtilityElement {
    return if (arcGISFeature.globalId == this.fromElement.globalId) {
        this.toElement
    } else {
        this.fromElement
    }
}
