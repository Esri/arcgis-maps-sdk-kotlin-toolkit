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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.arcgismaps.mapping.featureforms.FeatureFormSource
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the [UtilityAssociationsElement].
 *
 * @param element The [UtilityAssociationsFormElement] to represent.
 * @param scope The [CoroutineScope] to launch coroutines from.
 */
internal class UtilityAssociationsElementState(
    val element: UtilityAssociationsFormElement,
    scope: CoroutineScope
) : FormElementState(
    id = element.hashCode(),
    label = element.label,
    description = element.description,
    isVisible = element.isVisible
) {
    private var _loading: MutableState<Boolean> = mutableStateOf(true)

    private val _filters: SnapshotStateList<MutableFilterResult> = SnapshotStateList()

    /**
     * Indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = element.isEditable

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
    val filters: List<MutableFilterResult>
        get() = _filters

    private val _selectedFilterResult: MutableState<MutableFilterResult?> =
        mutableStateOf(null, policy = neverEqualPolicy())

    /**
     * The selected [UtilityAssociationsFilterResult] to display. Use [setSelectedFilterResult] to
     * set this value.
     */
    val selectedFilterResult: MutableFilterResult?
        get() = _selectedFilterResult.value

    private val _selectedGroupResult: MutableState<MutableGroupResult?> =
        mutableStateOf(null, policy = neverEqualPolicy())

    /**
     * The selected [UtilityAssociationGroupResult] to display. Use [setSelectedGroupResult] to
     * set this value.
     */
    val selectedGroupResult: MutableGroupResult?
        get() = _selectedGroupResult.value

    private val _selectedAssociationResult: MutableState<UtilityAssociationResult?> =
        mutableStateOf(null, policy = neverEqualPolicy())

    /**
     * The selected [UtilityAssociationResult] to display. Use [setSelectedAssociationResult] to
     * set this value.
     */
    val selectedAssociationResult: UtilityAssociationResult?
        get() = _selectedAssociationResult.value

    init {
        scope.launch {
            // fetch the associations filter results for the element
            refreshResults()
        }
    }

    /**
     * Fetches the latest filter results from the [UtilityAssociationsFormElement] and updates
     * the [filters] list.
     */
    suspend fun refreshResults() {
        element.fetchAssociationsFilterResults()
        _filters.clear()
        element.associationsFilterResults.forEach {
            val groupResults = it.groupResults.map { groupResult ->
                MutableGroupResult(
                    results = groupResult.associationResults,
                    name = groupResult.name,
                    source = groupResult.featureFormSource
                )
            }
            _filters += MutableFilterResult(
                filter = it.filter,
                groupResults = groupResults,
                resultCount = it.resultCount,
                onDelete = { association ->
                    // delete the association from the element when it is deleted from the state
                    element.deleteAssociation(association)
                }
            )
        }
        // update the selections as the filter results may have changed
        val updatedFilter = _filters.find { it.filter == selectedFilterResult?.filter }
        // if the filter was found, update the selected filter result
        if (updatedFilter != null) {
            _selectedFilterResult.value = updatedFilter
            // update the selected group result if it exists in the new filter results
            val updatedGroup = updatedFilter.groupResults.find {
                it.source == selectedGroupResult?.source
            }
            // update the selected group result to trigger recomposition
            // this may be null if the group no longer exists (e.g. all associations deleted)
            _selectedGroupResult.value = updatedGroup
        }
        _loading.value = false
    }

    /**
     * Sets the selected [UtilityAssociationsFilterResult] to display.
     */
    fun setSelectedFilterResult(filterResult: MutableFilterResult) {
        _selectedFilterResult.value = filterResult
    }

    /**
     * Sets the selected [UtilityAssociationGroupResult] to display.
     */
    fun setSelectedGroupResult(groupResult: MutableGroupResult?) {
        _selectedGroupResult.value = groupResult
    }

    /**
     * Sets the selected [UtilityAssociationResult] to display.
     */
    fun setSelectedAssociationResult(associationResult: UtilityAssociationResult?) {
        _selectedAssociationResult.value = associationResult
    }
}

/**
 * A mutable version of [UtilityAssociationGroupResult] that allows deleting associations.
 *
 * @param results The initial list of [UtilityAssociationResult] in this group.
 * @param name The name of the group.
 */
internal class MutableGroupResult(
    results: List<UtilityAssociationResult>,
    val name: String,
    val source : FeatureFormSource
) {
    private val _associationResults: SnapshotStateList<UtilityAssociationResult> =
        mutableStateListOf<UtilityAssociationResult>().apply { addAll(results) }

    /**
     * The list of [UtilityAssociationResult] in this group. This list is observable can be modified
     * by calling [delete].
     */
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

    /**
     * Sets a listener that is called when an association is deleted from this group.
     */
    fun setOnAssociationDeletedListener(listener: (UtilityAssociation) -> Unit) {
        onAssociationDeleted = listener
    }
}

/**
 * A mutable version of [UtilityAssociationsFilterResult] that allows deleting associations.
 *
 * @param filter The [UtilityAssociationsFilter] represented by this result.
 * @param groupResults The initial list of [MutableGroupResult] in this filter result.
 * @param resultCount The initial count of results in this filter result.
 * @param onDelete A callback that is called after an association is deleted.
 */
internal class MutableFilterResult(
    val filter: UtilityAssociationsFilter,
    groupResults: List<MutableGroupResult>,
    resultCount: Int,
    onDelete: (UtilityAssociation) -> Unit
) {

    /**
     * Backing list for the [groupResults] property.
     */
    private val _groupResults: SnapshotStateList<MutableGroupResult> =
        mutableStateListOf<MutableGroupResult>().apply {
            groupResults.forEach {
                // set a listener on each group result to handle deletions
                it.setOnAssociationDeletedListener { association ->
                    _resultCount.value -= 1
                    if (it.associationResults.isEmpty()) {
                        // when a group is empty, remove it from the list
                        _groupResults.remove(it)
                    }
                    onDelete(association)
                }
                add(it)
            }
        }

    /**
     * The list of [MutableGroupResult] in this filter result. This list is observable and
     * can be modified when associations are deleted.
     */
    val groupResults: List<MutableGroupResult>
        get() = _groupResults

    /**
     * Backing state for the [resultCount] property.
     */
    private val _resultCount: MutableState<Int> = mutableIntStateOf(resultCount)

    /**
     * The count of results in this filter result. This value is observable and is updated
     * when associations are deleted.
     */
    val resultCount: Int
        get() = _resultCount.value
}
