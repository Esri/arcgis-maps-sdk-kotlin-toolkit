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

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
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
    private val element: UtilityAssociationsFormElement,
    private val scope: CoroutineScope,
    private val evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : FormElementState(
    id = element.hashCode(),
    label = element.label,
    description = element.description,
    isVisible = element.isVisible
) {
    private var _loading: MutableState<Boolean> = mutableStateOf(true)

    private val _filters: SnapshotStateList<FilterResult> = SnapshotStateList()

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
    val filters: List<FilterResult>
        get() = _filters

    private val _selectedFilterResult: MutableState<FilterResult?> =
        mutableStateOf(null, policy = neverEqualPolicy())

    /**
     * The selected [UtilityAssociationsFilterResult] to display. Use [setSelectedFilterResult] to
     * set this value.
     */
    val selectedFilterResult: FilterResult?
        get() = _selectedFilterResult.value

    private val _selectedGroupResult: MutableState<GroupResult?> =
        mutableStateOf(null, policy = neverEqualPolicy())

    /**
     * The selected [UtilityAssociationGroupResult] to display. Use [setSelectedGroupResult] to
     * set this value.
     */
    val selectedGroupResult: GroupResult?
        get() = _selectedGroupResult.value

    /**
     * The selected [UtilityAssociationResult] to display. Use [setSelectedAssociationResult] to
     * set this value.
     */
    var selectedAssociationResult: UtilityAssociationResult? = null
        private set

    init {
        scope.launch {
            // fetch the associations filter results for the element
            refreshResults()
        }
    }

    suspend fun refreshResults() {
        element.fetchAssociationsFilterResults()
        _filters.clear()
        element.associationsFilterResults.forEach {
            val groupResults = mutableStateListOf<GroupResult>()
            groupResults += it.groupResults.map {
                GroupResult(it.associationResults, it.name)
            }
            _filters += FilterResult(
                it.filter,
                groupResults,
                it.resultCount
            ) { association ->
                element.deleteAssociation(association)
                scope.launch {
                    evaluateExpressions()
                }
            }
        }
        // update the selections as the filter results may have changed
        val updatedFilter = _filters.find { it == selectedFilterResult }
        // if the filter was found, update the selected filter result
        if (updatedFilter != null) {
            _selectedFilterResult.value = updatedFilter
            // update the selected group result if it exists in the new filter results
            val updatedGroup = updatedFilter.groupResults.find {
                it.name == selectedGroupResult?.name
            }
            if (updatedGroup != null) {
                // update the selected group result to trigger recomposition
                _selectedGroupResult.value = updatedGroup
            }
        }
        _loading.value = false
    }

    /**
     * Sets the selected [UtilityAssociationsFilterResult] to display.
     */
    fun setSelectedFilterResult(filterResult: FilterResult) {
        _selectedFilterResult.value = filterResult
    }

    /**
     * Sets the selected [UtilityAssociationGroupResult] to display.
     */
    fun setSelectedGroupResult(groupResult: GroupResult?) {
        _selectedGroupResult.value = groupResult
    }

    /**
     * Sets the selected [UtilityAssociationResult] to display.
     */
    fun setSelectedAssociationResult(associationResult: UtilityAssociationResult?) {
        selectedAssociationResult = associationResult
    }
}

internal class GroupResult(
    results: List<UtilityAssociationResult>,
    val name: String
) {
    private val _associationResults: SnapshotStateList<UtilityAssociationResult> =
        mutableStateListOf<UtilityAssociationResult>().apply { addAll(results) }

    val associationResults: List<UtilityAssociationResult>
        get() = _associationResults

    private var onAssociationDeleted: ((UtilityAssociation) -> Unit)? = null

    /**
     * Deletes the given [UtilityAssociation] from the list of association results.
     *
     * @return true if there are no more association results in this group after the deletion.
     */
    fun delete(association: UtilityAssociation): Boolean {
        val result = _associationResults.removeIf {
            it.association == association
        }
        if (result) {
            onAssociationDeleted?.invoke(association)
        }
        return associationResults.isEmpty()
    }

    fun setOnAssociationDeletedListener(listener: (UtilityAssociation) -> Unit) {
        onAssociationDeleted = listener
    }
}

internal class FilterResult(
    val filter: UtilityAssociationsFilter,
    val groupResults: SnapshotStateList<GroupResult>,
    resultCount: Int,
    onDelete: (UtilityAssociation) -> Unit
) {
    private val _resultCount: MutableState<Int> = mutableIntStateOf(resultCount)
    val resultCount: Int
        get() = _resultCount.value

    init {
        groupResults.forEach {
            it.setOnAssociationDeletedListener { association ->
                _resultCount.value -= 1
                if (it.associationResults.isEmpty()) {
                    // when a group is empty, remove it from the list
                    groupResults.remove(it)
                }
                onDelete(association)
            }
        }
    }

    override fun hashCode(): Int {
        return 31 * filter.title.hashCode() +
            filter.description.hashCode() +
            filter.filterType.hashCode() +
            filter.assetGroup.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterResult) return false

        // Deep comparison of the `filter` property
        if (filter.title != other.filter.title) return false
        if (filter.description != other.filter.description) return false
        if (filter.filterType != other.filter.filterType) return false
        if (filter.assetGroup != other.filter.assetGroup) return false
        if (filter.assetType != other.filter.assetType) return false
        return true
    }
}
