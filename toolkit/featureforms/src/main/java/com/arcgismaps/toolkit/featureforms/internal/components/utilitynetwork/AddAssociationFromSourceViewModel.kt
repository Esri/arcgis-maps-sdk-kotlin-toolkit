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
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureCandidate
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureSource
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class AddAssociationFromSourceViewModel(
    private val element: UtilityAssociationsFormElement,
    private val filter: UtilityAssociationsFilter,
    private val onAssociationAdded: () -> Unit
) : ViewModel() {

    private val _featureSources: MutableState<List<UtilityAssociationFeatureSource>> =
        mutableStateOf(emptyList())

    /**
     * A map of [UtilityAssociationsFilter] to a list of related [UtilityAssociationFeatureSource]
     * objects that can be used to create new associations for the filter. This is populated when
     * [fetchFeatureSources] is called for a specific filter.
     */
    val featureSources: List<UtilityAssociationFeatureSource>
        get() = _featureSources.value

    private val _selectedSourceIndex: MutableState<Int?> = mutableStateOf(null)

    /**
     * The index of the currently selected [UtilityAssociationFeatureSource] in the
     * [featureSources] list, or `null` if no source is selected.
     */
    val selectedSourceIndex: Int?
        get() = _selectedSourceIndex.value

    /**
     * A [Flow] of [PagingData] containing [UtilityAssociationFeatureCandidate] objects. This flow
     * is updated whenever the [selectedSourceIndex] changes. If no source is selected, this flow
     * will be empty.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val featureCandidateFlow: Flow<PagingData<UtilityAssociationFeatureCandidate>> = snapshotFlow {
        _selectedSourceIndex.value
    }.map { index ->
        index?.let {
            featureSources.getOrNull(index)
        }
    }.distinctUntilChanged().flatMapLatest { source ->
        if (source == null) {
            flowOf(PagingData.empty())
        } else {
            val pagingSource = AssociationFeatureCandidatePagingSource(
                featureSource = source,
                queryParamsProvider = {
                    QueryParameters().apply {
                        whereClause = "1=1"
                    }
                }
            )
            Pager(
                config = PagingConfig(
                    pageSize = 20
                ),
                pagingSourceFactory = { pagingSource }
            ).flow
        }
    }.cachedIn(viewModelScope)

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
     * Sets the selected source index to the given [index].
     *
     * @param index The index of the source to select.
     */
    fun selectSource(index: Int) {
        if (index in featureSources.indices) {
            _selectedSourceIndex.value = index
        } else {
            _selectedSourceIndex.value = null
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        fun Factory(
            element: UtilityAssociationsFormElement,
            filter: UtilityAssociationsFilter,
            onAssociationAdded: () -> Unit
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddAssociationFromSourceViewModel(
                    element,
                    filter,
                    onAssociationAdded
                )
            }
        }
    }
}

/**
 * A [PagingSource] that loads [UtilityAssociationFeatureCandidate] objects from a given
 * [UtilityAssociationFeatureSource] using provided [QueryParameters].
 *
 * @param featureSource The [UtilityAssociationFeatureSource] to load candidates from.
 * @param queryParamsProvider A lambda that provides the [QueryParameters] to use for each query.
 */
internal class AssociationFeatureCandidatePagingSource(
    private val featureSource: UtilityAssociationFeatureSource,
    private val queryParamsProvider: () -> QueryParameters
) : PagingSource<Int, UtilityAssociationFeatureCandidate>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UtilityAssociationFeatureCandidate> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val page = params.key ?: 0
                val pageSize = params.loadSize
                // Create query parameters with pagination
                val queryParams = queryParamsProvider().apply {
                    this.resultOffset = page * pageSize
                    this.maxFeatures = pageSize
                }
                // Query the feature source for candidates
                val result = featureSource.queryFeatures(queryParams).getOrThrow()
                val candidates = result.candidates
                // Determine the next and previous keys
                val nextKey = if (candidates.size < pageSize) null else page + 1
                val prevKey = if (page == 0) null else page - 1
                // return the loaded page
                LoadResult.Page(
                    data = candidates,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

    override fun getRefreshKey(state: PagingState<Int, UtilityAssociationFeatureCandidate>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
