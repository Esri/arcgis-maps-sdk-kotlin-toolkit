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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.data.FeatureSubtype
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureCandidate
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureOptions
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureSource
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import com.arcgismaps.utilitynetworks.UtilityAssetType
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterType
import com.arcgismaps.utilitynetworks.UtilityTerminal
import com.arcgismaps.utilitynetworks.UtilityTerminalConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val featureForm: FeatureForm,
    val element: UtilityAssociationsFormElement,
    val filter: UtilityAssociationsFilter,
    private val onAssociationAdded: suspend () -> Unit
) : ViewModel() {

    private val _featureSourcesFilterText: MutableState<String> = mutableStateOf("")

    /**
     * The current text used to filter the list of feature sources. This can be set via
     * [setFeatureSourcesFilterText].
     */
    val featureSourcesFilterText by _featureSourcesFilterText

    private val _assetTypesFilterText: MutableState<String> = mutableStateOf("")

    /**
     * The current text used to filter the list of asset types. This can be set via
     * [setAssetTypesFilterText].
     */
    val assetTypesFilterText by _assetTypesFilterText

    private val _associatedFeaturesFilterText: MutableState<String> = mutableStateOf("")

    /**
     * The current text used to filter the list of associated feature candidates. This can be set
     * via [setAssociatedFeaturesFilterText].
     */
    val associatedFeaturesFilterText by _associatedFeaturesFilterText

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


    private val _featureCandidatesUiState = mutableStateOf(
        FeatureCandidatesUiState(isLoading = true)
    )

    /**
     * The current state of feature candidates being loaded from the selected source and asset type.
     */
    val featureCandidatesUiState: State<FeatureCandidatesUiState> = _featureCandidatesUiState

    private val _newAssociationOptions =
        mutableStateOf<NewAssociationOptions?>(null)

    /**
     * Options for creating a new association based on the currently selected feature candidate.
     * This will be `null` if no candidate is selected or if the options have not been fetched yet.
     */
    val newAssociationOptions: NewAssociationOptions?
        get() = _newAssociationOptions.value

    /**
     * A flow that emits a list of [UtilityAssociationFeatureSource]s based on the current search text.
     */
    val filteredFeatureSources: StateFlow<List<UtilityAssociationFeatureSource>> = snapshotFlow {
        featureSourcesFilterText to featureSources
    }.map { (text, _) ->
        return@map if (text.isNotEmpty()) {
            filterFeatureSources(text)
        } else {
            _featureSources.value
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = _featureSources.value
    )

    /**
     * A flow that emits a list of [UtilityAssetType]s based on the current search text.
     */
    val filteredAssetTypes: StateFlow<List<UtilityAssetType>> = snapshotFlow {
        assetTypesFilterText to selectedSource
    }.map { (text, _) ->
        return@map if (text.isNotEmpty()) {
            filterAssetTypes(text)
        } else {
            _selectedSource.value?.assetTypes ?: emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = _selectedSource.value?.assetTypes ?: emptyList()
    )

    /**
     * A flow that emits a [FeatureCandidatesUiState] based on the current search text.
     */
    val filteredFeatureCandidatesUiState: StateFlow<FeatureCandidatesUiState> = snapshotFlow {
        associatedFeaturesFilterText to _featureCandidatesUiState.value
    }.map { (text, candidatesUiState) ->
        return@map if (text.isNotEmpty()) {
            val candidateList = candidatesUiState.candidates.toMutableList()
            var nextQueryParams = candidatesUiState.nextQueryParams
            var filteredList = filterFeatureCandidates(text)
            // Keep fetching additional pages while no candidates match the filter and a next page
            // (nextQueryParams) is available. Stop when matches appear or there are no more pages.
            while (filteredList.isEmpty() && nextQueryParams != null) {
                val source = _selectedSource.value ?: break
                _isSearchingForCandidates.value = true
                source.queryFeatures(nextQueryParams).onSuccess { result ->
                    // Append the newly fetched candidates to the existing list
                    candidateList += result.candidates
                    // Update the candidates list with the updated list
                    _featureCandidatesUiState.value = FeatureCandidatesUiState(
                        isLoading = false,
                        candidates = candidateList,
                        nextQueryParams = result.nextQueryParams
                    )
                    // Update for the next iteration, if needed
                    nextQueryParams = result.nextQueryParams
                    // Filter the updated list
                    filteredList = filterFeatureCandidates(text)
                }.onFailure {
                    // If there's an error, set the error state and break the loop
                    _featureCandidatesUiState.value = FeatureCandidatesUiState(
                        isLoading = false,
                        error = it
                    )
                    break
                }
            }
            _isSearchingForCandidates.value = false
            FeatureCandidatesUiState(
                isLoading = false,
                candidates = filteredList,
                nextQueryParams = nextQueryParams
            )
        } else {
            _featureCandidatesUiState.value
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = FeatureCandidatesUiState(isLoading = true)
    )

    private val _isSearchingForCandidates = mutableStateOf(false)

    /**
     * Indicates whether the ViewModel is currently searching for more feature candidates when there
     * are more pages of results to fetch.
     */
    val isSearchingForCandidates: Boolean
        get() = _isSearchingForCandidates.value

    /**
     * Backing list for [fields].
     */
    private val _fields = mutableStateOf<List<Field>>(emptyList())

    /**
     * A list of fields for the current source that can be used for attribute filtering.
     */
    val fields: List<Field>
        get() = _fields.value

    /**
     * Manages the state of attribute filters applied to the feature candidates.
     */
    var attributeFilterStateManager = FilterStateManager(onApplyFilter = ::applyAttributeFilters)
        private set

    private var _areAttributeFiltersApplied: MutableState<Boolean> = mutableStateOf(false)

    /**
     * Indicates whether any attribute filters are currently applied to the feature candidates.
     */
    val areAttributeFiltersApplied: Boolean
        get() = _areAttributeFiltersApplied.value

    /**
     * Indicates whether the ViewModel is currently fetching the next page of feature candidates.
     * This is used to prevent multiple simultaneous fetches.
     */
    private var _isFetchingNextPage = false

    init {
        viewModelScope.launch {
            // Whenever the selected source or asset type changes, reload the feature candidates
            snapshotFlow {
                _selectedSource.value to _selectedAssetType.value
            }.distinctUntilChanged().collect { pair ->
                val source = pair.first
                val assetType = pair.second
                // If no source, asset type is selected return an error state
                if (source == null || assetType == null) {
                    _featureCandidatesUiState.value = FeatureCandidatesUiState(
                        isLoading = false,
                        error = IllegalStateException(
                            "No source or asset type selected"
                        )
                    )
                } else {
                    _featureCandidatesUiState.value = FeatureCandidatesUiState(isLoading = true)
                    // Load the candidates from the selected source and asset type
                    source.queryFeatures(
                        assetType = assetType
                    ).onSuccess { result ->
                        // Get the first feature from the results to extract the fields and the subtype
                        val firstFeature = result.candidates.firstOrNull()?.feature
                        // Get the available fields from the table
                        val supportedFields = firstFeature?.featureTable?.fields?.getSupportedFields() ?: emptyList()
                        // Apply any subtype overrides to the fields
                        val subtype = firstFeature?.getFeatureSubtype()
                        _fields.value = if (subtype != null)
                            filterFieldsAndApplySubtypeOverrides(supportedFields, subtype)
                        else
                            supportedFields

                        _featureCandidatesUiState.value = FeatureCandidatesUiState(
                            isLoading = false,
                            candidates = result.candidates,
                            nextQueryParams = result.nextQueryParams
                        )
                    }.onFailure { error ->
                        // Clear fields on error
                        _fields.value = emptyList()
                        _featureCandidatesUiState.value = FeatureCandidatesUiState(
                            isLoading = false,
                            error = error
                        )
                    }
                    // Create a new filter state manager for the new source/asset type
                    attributeFilterStateManager = FilterStateManager(
                        onApplyFilter = ::applyAttributeFilters
                    )
                    // Reset whether filters are applied
                    _areAttributeFiltersApplied.value = false
                }
            }
        }
    }

    /**
     * Filters the list of [UtilityAssociationFeatureSource] objects based on the provided
     * [filterString], returning only those whose names contain the filter string (case-insensitive).
     */
    private fun filterFeatureSources(filterString: String): List<UtilityAssociationFeatureSource> {
        return _featureSources.value.filter { featureSource ->
            featureSource.name.contains(filterString, ignoreCase = true)
        }
    }

    /**
     * Filters the list of [UtilityAssetType] objects from the currently selected source based on
     * the provided [filterString], returning only those whose names contain the filter string
     * (case-insensitive).
     */
    private fun filterAssetTypes(filterString: String): List<UtilityAssetType> {
        return _selectedSource.value?.assetTypes?.filter { assetType ->
            assetType.name.contains(filterString, ignoreCase = true)
        } ?: emptyList()
    }

    /**
     * Filters the list of [UtilityAssociationFeatureCandidate] objects based on the provided
     * [filterString], returning only those whose titles contain the filter string (case-insensitive).
     */
    private fun filterFeatureCandidates(filterString: String): List<UtilityAssociationFeatureCandidate> {
        return _featureCandidatesUiState.value.candidates.filter { candidate ->
            candidate.title.contains(filterString, ignoreCase = true)
        }
    }

    /**
     * Sets the text for [featureSourcesFilterText].
     */
    fun setFeatureSourcesFilterText(text: String) {
        _featureSourcesFilterText.value = text
    }

    /**
     * Sets the text for [assetTypesFilterText].
     */
    fun setAssetTypesFilterText(text: String) {
        _assetTypesFilterText.value = text
    }

    /**
     * Sets the text for [associatedFeaturesFilterText].
     */
    fun setAssociatedFeaturesFilterText(text: String) {
        _associatedFeaturesFilterText.value = text
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
     * Sets the currently selected [UtilityAssociationFeatureCandidate] and fetches the
     * [UtilityAssociationFeatureOptions] for the candidate feature. [newAssociationOptions] will
     * be updated once the options are fetched. This is an asynchronous operation.
     *
     * @param candidate The [UtilityAssociationFeatureCandidate] to select.
     */
    fun selectFeatureCandidate(candidate: UtilityAssociationFeatureCandidate) {
        viewModelScope.launch {
            element.getOptionsForAssociationCandidate(candidate.feature).onSuccess { options ->
                val (fromEndPoint, toEndPoint) = when (filter.filterType) {
                    is UtilityAssociationsFilterType.Connectivity -> {
                        AssociationEndpoint(
                            featureForm.title.value,
                            options.formFeatureTerminalConfiguration
                        ) to
                            AssociationEndpoint(
                                candidate.title,
                                options.candidateFeatureTerminalConfiguration
                            )
                    }

                    is UtilityAssociationsFilterType.Container,
                    is UtilityAssociationsFilterType.Structure -> {
                        AssociationEndpoint(name = candidate.title) to
                            AssociationEndpoint(featureForm.title.value)
                    }

                    else -> {
                        AssociationEndpoint(featureForm.title.value) to
                            AssociationEndpoint(candidate.title)
                    }
                }

                _newAssociationOptions.value = NewAssociationOptions(
                    candidate = candidate,
                    fromElement = fromEndPoint.name,
                    fromTerminalConfiguration = fromEndPoint.terminalConfiguration,
                    toElement = toEndPoint.name,
                    toTerminalConfiguration = toEndPoint.terminalConfiguration,
                    isPercentAlongValid = options.isFractionAlongEdgeValid,
                    type = filter.filterType
                )
            }
        }
    }

    /**
     * Adds a new association to the [element] using the provided parameters. If the proper
     * parameters for the association type are not provided, the result will be a failure.
     *
     * @param isContainmentVisible Whether the containment is visible. This is only applicable for
     * [UtilityAssociationsFilterType.Container] and [UtilityAssociationsFilterType.Content] types.
     * @param percentAlong the value expected is a fraction between 0 and 1 rather than a percentage
     *                      for connectivity associations.
     * @param fromTerminalId The terminal ID on the feature being edited.
     * @param toTerminalId The terminal ID on the candidate feature.
     * @return A [Result] containing the [UtilityAssociationResult] if the association was
     * successfully added, or an error if the operation failed.
     */
    suspend fun addAssociation(
        isContainmentVisible: Boolean,
        percentAlong: Double? = null,
        fromTerminalId: Int? = null,
        toTerminalId: Int? = null,
    ): Result<UtilityAssociationResult> = runCatching {
        val feature = newAssociationOptions?.candidate?.feature
        require(feature != null) {
            "No feature candidate selected"
        }
        val fromTerminal = fromTerminalId?.let { id ->
            newAssociationOptions?.fromTerminalConfiguration.getTerminalById(id)
        }
        val toTerminal = toTerminalId?.let { id ->
            newAssociationOptions?.toTerminalConfiguration.getTerminalById(id)
        }
        // First check if we can add an association to the feature with the provided filter
        val canAddAssociation = element.canAddAssociation(
            feature,
            filter
        ).getOrThrow()
        if (canAddAssociation.not()) {
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
                        if (percentAlong != null) {
                            element.addAssociation(
                                feature = feature,
                                filter = filter,
                                fractionAlongEdge = percentAlong,
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
                        if (percentAlong != null) {
                            element.addAssociation(
                                feature = feature,
                                filter = filter,
                                fractionAlongEdge = percentAlong,
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

                    // no terminals, but percent along provided
                    percentAlong != null -> {
                        // edge to edge
                        element.addAssociation(
                            feature = feature,
                            filter = filter,
                            fractionAlongEdge = percentAlong
                        )
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
        // Clear previously set filter text
        setAssetTypesFilterText("")
    }

    /**
     * Sets the currently selected [UtilityAssetType].
     */
    fun selectAssetType(assetType: UtilityAssetType?) {
        _selectedAssetType.value = assetType
        // Clear previously set filter text
        setAssociatedFeaturesFilterText("")
    }

    /**
     * Loads more feature candidates from the selected source and asset type if there are more
     * candidates to fetch. This method will not fetch more candidates if a fetch is already in
     * progress or if there is an active search filter.
     *
     * This is not thread-safe as it is intended to be called from the ui thread only.
     */
    suspend fun loadMoreFeatureCandidates() {
        // Only fetch the next page if not already fetching and there's no active search filter
        if (_isFetchingNextPage.not() && featureSourcesFilterText.isEmpty()) {
            val source = _selectedSource.value ?: return
            val assetType = _selectedAssetType.value ?: return
            val nextQueryParams = _featureCandidatesUiState.value.nextQueryParams ?: return
            _isFetchingNextPage = true
            source.queryFeatures(
                assetType = assetType,
                parameters = nextQueryParams
            ).onSuccess { result ->
                // Append the newly fetched candidates to the existing list
                val updatedCandidates =
                    _featureCandidatesUiState.value.candidates + result.candidates
                // Update the candidates list with the updated list
                _featureCandidatesUiState.value = FeatureCandidatesUiState(
                    isLoading = false,
                    candidates = updatedCandidates,
                    nextQueryParams = result.nextQueryParams
                )
            }
            _isFetchingNextPage = false
        }
    }

    private suspend fun applyAttributeFilters(whereClause: String): Result<Unit> {
        val source = _selectedSource.value ?: return Result.failure(
            IllegalStateException("No source selected")
        )
        val assetType = _selectedAssetType.value ?: return Result.failure(
            IllegalStateException("No asset type selected")
        )
        val queryParams = QueryParameters().apply {
            this.whereClause = whereClause
        }
        val result = source.queryFeatures(
            assetType = assetType,
            parameters = queryParams
        ).onSuccess { result ->
            _featureCandidatesUiState.value = FeatureCandidatesUiState(
                isLoading = false,
                candidates = result.candidates,
                nextQueryParams = result.nextQueryParams
            )
            // Update whether filters are applied based on where clause being non-empty
            _areAttributeFiltersApplied.value = whereClause.isNotEmpty()
        }
        return if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(
                result.exceptionOrNull() ?: Exception("Unknown error")
            )
        }
    }

    private fun filterFieldsAndApplySubtypeOverrides(fields: List<Field>, subtype: FeatureSubtype): List<Field> {
        val filteredFields = fields.filterNot {
            it.name.equals("ASSETGROUP", ignoreCase = true) ||
                    it.name.equals("ASSETTYPE", ignoreCase = true)
        }.toMutableList()

        subtype.fieldOverrides.forEach { overrideField ->
            val index = filteredFields.indexOfFirst { it.name.equals(overrideField.name, ignoreCase = true) }
            if (index != -1 && overrideField.domain != null) {
                filteredFields[index] = overrideField
            }
        }
        return filteredFields
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
 * @param fromElement The name of the "from" element in the association.
 * @param fromTerminalConfiguration The [UtilityTerminalConfiguration] for the "from" element,
 * if applicable.
 * @param toElement The name of the "to" element in the association.
 * @param toTerminalConfiguration The [UtilityTerminalConfiguration] for the "to" element,
 * if applicable.
 * @param isPercentAlongValid Whether the percent along is valid for connectivity
 * associations.
 * @param type The [UtilityAssociationsFilterType] defining the type of association to create
 */
internal data class NewAssociationOptions(
    val candidate: UtilityAssociationFeatureCandidate,
    val fromElement: String,
    val fromTerminalConfiguration: UtilityTerminalConfiguration?,
    val toElement: String,
    val toTerminalConfiguration: UtilityTerminalConfiguration?,
    val isPercentAlongValid: Boolean,
    val type: UtilityAssociationsFilterType
)

/**
 * Represents the UI state for loading feature candidates, including loading status,
 * any errors encountered, and the list of loaded candidates.
 *
 * @param isLoading Indicates whether the feature candidates are currently being loaded.
 * @param error An optional [Throwable] representing any error that occurred during loading.
 * @param candidates A list of loaded [UtilityAssociationFeatureCandidate] objects.
 * @param nextQueryParams Optional [QueryParameters] for fetching the next set of candidates, if any.
 */
internal data class FeatureCandidatesUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val candidates: List<UtilityAssociationFeatureCandidate> = emptyList(),
    val nextQueryParams: QueryParameters? = null
)

/**
 * Holds information about an association endpoint, including its name and optional terminal
 * configuration.
 */
private data class AssociationEndpoint(
    val name: String,
    val terminalConfiguration: UtilityTerminalConfiguration? = null
)

/**
 * Returns the [UtilityTerminal] with the specified [id] from the [UtilityTerminalConfiguration].
 */
internal fun UtilityTerminalConfiguration?.getTerminalById(id: Int): UtilityTerminal? {
    return this?.terminals?.find { it.terminalId == id }
}

/**
 * Filters the list of [Field] objects to include only those that are supported for attribute
 * filtering.
 */
private fun List<Field>.getSupportedFields(): List<Field> {
    return filter { field ->
        field.fieldType == FieldType.Text ||
            field.fieldType == FieldType.Oid ||
            field.fieldType.isNumeric
    }
}
