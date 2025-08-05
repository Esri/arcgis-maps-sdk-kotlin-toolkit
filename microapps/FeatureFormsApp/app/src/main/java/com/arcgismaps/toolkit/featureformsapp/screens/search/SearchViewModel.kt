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

package com.arcgismaps.toolkit.featureformsapp.screens.search

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.portal.PortalQueryParameters
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PortalItemRepository,
) : ViewModel() {

    private val _searchText: MutableState<String> = mutableStateOf("")
    val searchText by _searchText

    private val _isSearching: MutableState<Boolean> = mutableStateOf(false)
    val isSearching by _isSearching

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val items: StateFlow<List<PortalItem>> = snapshotFlow {
        searchText
    }.debounce(500L).mapLatest { text ->
        return@mapLatest if (text.isNotEmpty()) {
            _isSearching.value = true
            Log.e("TAG", "searching with: $text")
            searchOnPortal(text).also {
                _isSearching.value = false
            }
        } else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    /**
     * Sets the text for [searchText].
     */
    fun setSearchText(text: String) {
        _searchText.value = text
    }

    /**
     * Selects an item and invokes the callback with the item's ID.
     */
    fun selectItem(item: PortalItem) {
        repository.setActivePortalItem(item)
    }

    /**
     * Searches for items on the portal based on the provided search query.
     */
    private suspend fun searchOnPortal(searchQuery: String): List<PortalItem> {
        val queryParams = PortalQueryParameters.items(
            types = listOf(PortalItemType.WebMap),
            searchQuery = "title:$searchQuery",
        )
        val searchResults = repository.searchItems(queryParams).getOrNull()
        return searchResults?.results ?: emptyList()
    }
}
