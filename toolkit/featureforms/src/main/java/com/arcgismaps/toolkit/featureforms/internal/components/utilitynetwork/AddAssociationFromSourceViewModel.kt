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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureCandidate
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureOptions
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureSource
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.utilitynetworks.UtilityAssetType
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterType
import com.arcgismaps.utilitynetworks.UtilityTerminal
import com.arcgismaps.utilitynetworks.UtilityTerminalConfiguration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A [ViewModel] that manages the state for adding utility associations from a selected feature
 * source. This includes fetching available feature sources, managing the selected source and
 * asset type, loading feature candidates, and creating new associations based on user input.
 *
 * @param featureForm The [FeatureForm] containing the utility associations form element.
 * @param element The [UtilityAssociationsFormElement] to which new associations will be added.
 * @param filter The [UtilityAssociationsFilter] defining the type of association to add.
 * @param onAssociationAdded A callback that is invoked whenever a new association is successfully
 * added. This can be used to refresh the UI or perform other actions in response to the addition.
 */
internal class AddAssociationFromSourceViewModel(
    val featureForm : FeatureForm,
    private val element: UtilityAssociationsFormElement,
    private val filter: UtilityAssociationsFilter,
    private val onAssociationAdded: suspend () -> Unit
) : ViewModel() {

    private val _associatedFeaturesFilterQuery: MutableState<String> = mutableStateOf("")
    val associatedFeaturesFilterQuery: String
        get() = _associatedFeaturesFilterQuery.value

    private var allFeatureCandidates: List<UtilityAssociationFeatureCandidate> = emptyList()

    fun onAssociatedFeaturesFilterQueryChanged(query: String) {
        _associatedFeaturesFilterQuery.value = query
        // If the query is blank, show all candidates
        val filteredCandidates = if (query.isBlank()) {
            allFeatureCandidates
        } else {
            allFeatureCandidates.filter { it.title.contains(query, ignoreCase = true) }
        }
        _featureCandidates.value = _featureCandidates.value.copy(candidates = filteredCandidates)
    }

    private val _featureSources: MutableState<List<UtilityAssociationFeatureSource>> =
        mutableStateOf(emptyList())

    /**
     * A map of [UtilityAssociationsFilter] to a list of related [UtilityAssociationFeatureSource]
     * objects that can be used to create new associations for the filter. This is populated when
     * [fetchFeatureSources] is called for a specific filter.
     */
    val featureSources: List<UtilityAssociationFeatureSource>
        get() = _featureSources.value

    private val _selectedSource: MutableState<UtilityAssociationFeatureSource?> =
        mutableStateOf(null)

    /**
     * The currently selected [UtilityAssociationFeatureSource], or `null` if no source is selected.
     */
    val selectedSource: UtilityAssociationFeatureSource?
        get() = _selectedSource.value

    private val _selectedAssetType: MutableState<UtilityAssetType?> =
        mutableStateOf(null)

    /**
     * The currently selected [UtilityAssetType], or `null` if no asset type is selected.
     */
    val selectedAssetType: UtilityAssetType?
        get() = _selectedAssetType.value


    private val _featureCandidates = mutableStateOf(
        FeatureCandidatesUiState(isLoading = true)
    )

    /**
     * The current state of feature candidates being loaded from the selected source and asset type.
     */
    val featureCandidates: State<FeatureCandidatesUiState> = _featureCandidates

    private val _newAssociationOptions =
        mutableStateOf<NewAssociationOptions?>(null)

    /**
     * Options for creating a new association based on the currently selected feature candidate.
     * This will be `null` if no candidate is selected or if the options have not been fetched yet.
     */
    val newAssociationOptions: NewAssociationOptions?
        get() = _newAssociationOptions.value

    init {
        viewModelScope.launch {
            // Whenever the selected source or asset type changes, reload the feature candidates
            snapshotFlow {
                _selectedAssetType.value
            }.distinctUntilChanged().collect { assetType ->
                val source = selectedSource
                // If no source is selected, return an empty list
                if (source == null || assetType == null) {
                    _featureCandidates.value = FeatureCandidatesUiState(
                        isLoading = false,
                        error = IllegalStateException(
                            "No source or asset type selected"
                        )
                    )
                } else {
                    _featureCandidates.value = FeatureCandidatesUiState(isLoading = true)
                    // Load the candidates from the selected source and asset type
                    source.queryFeatures(
                        assetType = assetType
                    ).onSuccess { result ->
                        allFeatureCandidates = result.candidates
                        _featureCandidates.value = FeatureCandidatesUiState(
                            isLoading = false,
                            candidates = result.candidates
                        )
                    }.onFailure { error ->
                        _featureCandidates.value = FeatureCandidatesUiState(
                            isLoading = false,
                            error = error
                        )
                    }
                }
            }
        }
    }

    /**
     * Fetches the list of [UtilityAssociationFeatureSource] objects that can be used to create
     * new associations for the given [filter] and stores them in the [featureSources] map.
     * If the sources have already been fetched for the filter, this method does nothing.
     */
    suspend fun fetchFeatureSources() {
        if (_featureSources.value.isNotEmpty()) {
            // already fetched
            return
        }
        element.getAssociationFeatureSources(filter).onSuccess { sources ->
            _featureSources.value = sources
        }
    }

    /**
     * Sets the currently selected [UtilityAssociationFeatureCandidate].
     *
     * @param candidate The [UtilityAssociationFeatureCandidate] to select, or `null` to clear the
     * selection.
     */
    suspend fun selectFeatureCandidate(candidate: UtilityAssociationFeatureCandidate) {
        element.getOptionsForAssociationCandidate(candidate.feature).onSuccess {
            _newAssociationOptions.value = NewAssociationOptions(
                candidate = candidate,
                options = it,
                type = filter.filterType
            )
        }
    }

    /**
     * Adds a new association to the [element] using the provided parameters. If the proper
     * parameters for the association type are not provided, the result will be a failure.
     *
     * @param isContainmentVisible Whether the containment is visible. This is only applicable for
     * [UtilityAssociationsFilterType.Container] and [UtilityAssociationsFilterType.Content] types.
     * @param fractionAlongEdge The fraction along the edge for connectivity associations.
     * @param fromTerminalId The terminal ID on the feature being edited.
     * @param toTerminalId The terminal ID on the candidate feature.
     * @return A [Result] containing the [UtilityAssociationResult] if the association was
     * successfully added, or an error if the operation failed.
     */
    suspend fun addAssociation(
        isContainmentVisible: Boolean,
        fractionAlongEdge: Float? = null,
        fromTerminalId : Int? = null,
        toTerminalId : Int? = null,
    ) : Result<UtilityAssociationResult> = runCatching {
        val feature = newAssociationOptions?.candidate?.feature
        require(feature != null) {
            "No feature candidate selected"
        }
        val fromTerminal = fromTerminalId?.let { id ->
            newAssociationOptions?.options?.formFeatureTerminalConfiguration.getTerminalById(id)
        }
        val toTerminal = toTerminalId?.let { id ->
            newAssociationOptions?.options?.candidateFeatureTerminalConfiguration.getTerminalById(id)
        }
        // First check if we can add an association to the feature with the provided filter
        val canAddAssociation = element.canAddAssociation(
            feature,
            filter
        ).getOrThrow()
        if (canAddAssociation.not())  {
            throw IllegalStateException("Cannot add an association to the provided feature")
        }
        // Capture the result of adding the association based on the filter type
        val result = when (filter.filterType) {
            is UtilityAssociationsFilterType.Container,
            is UtilityAssociationsFilterType.Content -> {
                element.addAssociation(
                    feature,
                    filter = filter,
                    isContainmentVisible = isContainmentVisible
                )
            }

            is UtilityAssociationsFilterType.Structure,
            is UtilityAssociationsFilterType.Attachment -> {
                element.addAssociation(feature, filter)
            }

            is UtilityAssociationsFilterType.Connectivity -> {
                when {
                    // If both terminals are provided
                    fromTerminal != null && toTerminal != null -> {
                        // junction to junction
                        element.addAssociation(
                            feature = feature,
                            featureTerminal = toTerminal,
                            filter = filter,
                            currentFeatureTerminal = fromTerminal
                        )
                    }
                    // junction (with the terminal) to edge
                    fromTerminal != null -> {
                        // these two methods can be defaulted at the sdk level
                        if (fractionAlongEdge != null) {
                            element.addAssociation(
                                feature = feature,
                                filter = filter,
                                fractionAlongEdge = fractionAlongEdge.toDouble(),
                                terminal = fromTerminal
                            )
                        } else {
                            element.addAssociation(
                                feature = feature,
                                featureTerminal = null,
                                filter = filter,
                                currentFeatureTerminal = fromTerminal
                            )
                        }
                    }

                    // edge to junction (with the terminal)
                    toTerminal != null -> {
                        // these two methods can be defaulted at the sdk level
                        if (fractionAlongEdge != null) {
                            element.addAssociation(
                                feature = feature,
                                filter = filter,
                                fractionAlongEdge = fractionAlongEdge.toDouble(),
                                terminal = toTerminal
                            )
                        } else {
                            element.addAssociation(
                                feature = feature,
                                featureTerminal = toTerminal,
                                filter = filter,
                                currentFeatureTerminal = null
                            )
                        }
                    }

                    // edge to edge or junction to junction without terminals
                    else -> {
                        element.addAssociation(
                            feature = feature,
                            filter = filter
                        )
                    }
                }
            }
        }
        if (result.isSuccess) {
            // If successful, raise the callback to notify the association was added so the receiver
            // can refresh its state
            onAssociationAdded()
        }
        result.getOrThrow()
    }

    /**
     * Sets the currently selected [UtilityAssociationFeatureSource].
     *
     * @param source The [UtilityAssociationFeatureSource] to select, or `null` to clear the
     * selection.
     */
    fun selectSource(source: UtilityAssociationFeatureSource?) {
        _selectedSource.value = source
    }

    /**
     * Sets the currently selected [UtilityAssetType].
     */
    fun selectAssetType(assetType: UtilityAssetType?) {
        _selectedAssetType.value = assetType
    }

    fun clearAssociatedFeaturesFilterQuery() {
        _associatedFeaturesFilterQuery.value = ""
    }

    companion object {
        fun Factory(
            featureForm: FeatureForm,
            element: UtilityAssociationsFormElement,
            filter: UtilityAssociationsFilter,
            onAssociationAdded: suspend () -> Unit
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddAssociationFromSourceViewModel(
                    featureForm,
                    element,
                    filter,
                    onAssociationAdded
                )
            }
        }
    }
}

/**
 * Holds the options for creating a new association based on a selected candidate feature.
 *
 * @param candidate The selected [UtilityAssociationFeatureCandidate].
 * @param options The [UtilityAssociationFeatureOptions] for the candidate feature.
 * @param type The [UtilityAssociationsFilterType] defining the type of association to create
 */
internal data class NewAssociationOptions(
    val candidate: UtilityAssociationFeatureCandidate,
    val options: UtilityAssociationFeatureOptions,
    val type: UtilityAssociationsFilterType
)

internal data class FeatureCandidatesUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val candidates: List<UtilityAssociationFeatureCandidate> = emptyList()
)

internal fun UtilityTerminalConfiguration?.getTerminalById(id: Int): UtilityTerminal? {
    return this?.terminals?.find { it.terminalId == id }
}
