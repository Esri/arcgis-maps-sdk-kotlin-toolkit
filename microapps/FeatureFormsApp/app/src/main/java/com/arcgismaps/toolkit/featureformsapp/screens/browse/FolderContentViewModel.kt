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

package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.navigation.Navigator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedFactory
interface FolderContentViewModelFactory {
    fun create(folder: PortalFolder): FolderContentViewModel
}

@HiltViewModel(assistedFactory = FolderContentViewModelFactory::class)
class FolderContentViewModel @AssistedInject constructor(
    @Assisted
    val folder: PortalFolder,
    private val repository: PortalItemRepository
) : ViewModel() {

    val items = repository.observe(folder.folderId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    var isLoading by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            // if the data is empty, refresh it
            // this is used to identify first launch
            if (repository.getItemCount(folder.folderId) == 0) {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            repository.getItemsInFolder(folder)
            isLoading = false
        }
    }

    fun setPortalItem(portalItem: PortalItem) {
        repository.setActivePortalItem(portalItem)
    }
}
