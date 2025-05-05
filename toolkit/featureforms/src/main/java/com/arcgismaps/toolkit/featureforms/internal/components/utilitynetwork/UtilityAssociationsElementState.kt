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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for the [UtilityAssociationsElement].
 *
 * @param element The [UtilityAssociationsFormElement] to represent.
 * @param scope The [CoroutineScope] to launch coroutines from.
 */
internal class UtilityAssociationsElementState(
    element: UtilityAssociationsFormElement,
    scope: CoroutineScope
) : FormElementState(
    id = element.hashCode(),
    label = element.label,
    description = element.description,
    isVisible = element.isVisible
) {
    private var _loading: MutableState<Boolean> = mutableStateOf(true)

    private var _filters: MutableState<List<UtilityAssociationsFilterResult>> =
        mutableStateOf(emptyList())

    /**
     * Indicates if the state is loading data to fetch the filters [filters].
     *
     * This property is observable and if used within a composition it will be notified on every change.
     */
    val loading: Boolean
        get() = _loading.value

    /**
     * The list of [UtilityAssociationsFilterResult] to display. This is empty until the data is fetched
     * as part of the initialization.
     *
     * This property is observable and if used within a composition it will be notified on every change.
     */
    val filters: List<UtilityAssociationsFilterResult>
        get() = _filters.value

    /**
     * The selected [UtilityAssociationsFilterResult] to display. Use [setSelectedFilterResult] to
     * set this value.
     */
    var selectedFilterResult : UtilityAssociationsFilterResult? = null
        private set

    /**
     * The selected [UtilityAssociationGroupResult] to display. Use [setSelectedGroupResult] to
     * set this value.
     */
    var selectedGroupResult : UtilityAssociationGroupResult? = null
        private set

    init {
        scope.launch {
            // fetch the associations filter results for the element
            element.fetchAssociationsFilterResults()
            _filters.value = element.associationsFilterResults.filter {
                // filter out the results that have no associations
                it.resultCount > 0
            }
            _loading.value = false
        }
    }

    /**
     * Sets the selected [UtilityAssociationsFilterResult] to display.
     */
    fun setSelectedFilterResult(filterResult: UtilityAssociationsFilterResult) {
        selectedFilterResult = filterResult
    }

    /**
     * Sets the selected [UtilityAssociationGroupResult] to display.
     */
    fun setSelectedGroupResult(groupResult: UtilityAssociationGroupResult) {
        selectedGroupResult = groupResult
    }
}
