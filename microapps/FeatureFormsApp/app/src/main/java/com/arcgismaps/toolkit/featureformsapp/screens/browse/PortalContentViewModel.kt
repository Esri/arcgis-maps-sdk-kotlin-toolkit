/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.navigation.NavigationRoute
import com.arcgismaps.toolkit.featureformsapp.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapListUIState(
    val isLoading: Boolean,
    val items: List<PortalItem>,
    val folders: List<PortalFolder>
)

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class PortalContentViewModel @Inject constructor(
    private val portalItemRepository: PortalItemRepository,
    private val navigator: Navigator
) : ViewModel() {

    private val _username = mutableStateOf<String?>(null)

    /**
     * Username of the currently logged in user.
     */
    val username: String?
        get() = _username.value

    // State flow to keep track of current loading state
    private val _isLoading = MutableStateFlow(false)

    // State flow that combines the _isLoading and the PortalItemUseCase data flow to create
    // a MapListUIState
    val uiState: StateFlow<MapListUIState> =
        combine(
            _isLoading,
            portalItemRepository.observe(null),
            portalItemRepository.observeFolders()
        ) { isLoading, portalItemData, folderData ->
            MapListUIState(isLoading, portalItemData, folderData)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),
            initialValue = MapListUIState(true, emptyList(), emptyList())
        )

    init {
        viewModelScope.launch {
            _username.value = portalItemRepository.getUsername()
            // if the data is empty, refresh it
            // this is used to identify first launch
            if (portalItemRepository.getItemCount(null) == 0) {
                refresh()
            }
        }
    }

    /**
     * Refreshes the data.
     */
    fun refresh() {
        if (!_isLoading.value) {
            viewModelScope.launch {
                _isLoading.emit(true)
                portalItemRepository.refresh()
                _isLoading.emit(false)
            }
        }
    }

    /**
     * Sets the active portal item to the provided [portalItem].
     */
    fun setPortalItem(portalItem: PortalItem) {
        portalItemRepository.setActivePortalItem(portalItem)
    }

    /**
     * Signs out the user and navigates to the login screen.
     */
    fun signOut() {
        viewModelScope.launch {
            portalItemRepository.signOut()
            // add a artificial delay before navigating screens
            delay(500)
            navigator.navigateTo(NavigationRoute.Login)
        }
    }
}
