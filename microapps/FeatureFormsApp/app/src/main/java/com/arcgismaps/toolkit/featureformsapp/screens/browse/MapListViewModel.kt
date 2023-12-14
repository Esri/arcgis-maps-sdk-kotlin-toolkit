package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
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
    val searchText: String,
    val data: List<PortalItem>
)

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
    private val portalItemRepository: PortalItemRepository,
    private val portalSettings: PortalSettings,
    private val navigator: Navigator
) : ViewModel() {

    // State flow to keep track of current loading state
    private val _isLoading = MutableStateFlow(false)

    private val _searchText = MutableStateFlow("")

    // State flow that combines the _isLoading and the PortalItemUseCase data flow to create
    // a MapListUIState
    val uiState: StateFlow<MapListUIState> =
        combine(
            _isLoading,
            _searchText,
            portalItemRepository.observe()
        ) { isLoading, searchText, portalItemData ->
            val data = portalItemData.filter {
                searchText.isEmpty()
                    || it.title.uppercase().contains(searchText.uppercase())
                    || it.itemId.contains(searchText)
            }
            MapListUIState(isLoading, searchText, data)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),
            initialValue = MapListUIState(true, "", emptyList())
        )

    init {
        viewModelScope.launch {
            // if the data is empty, refresh it
            // this is used to identify first launch
            if (portalItemRepository.getItemCount() == 0) {
                refresh()
            }
        }
    }

    fun getUsername(): String {
        val credential =
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.getCredential(
                portalSettings.getPortalUrl()
            )
        return credential?.username ?: ""
    }

    /**
     * Refreshes the data.
     */
    fun refresh() {
        if (!_isLoading.value) {
            viewModelScope.launch {
                _isLoading.emit(true)
                portalItemRepository.refresh(
                    portalSettings.getPortalUrl(),
                    portalSettings.getPortalConnection()
                )
                _isLoading.emit(false)
            }
        }
    }

    fun filterPortalItems(filterText: String) {
        _searchText.value = filterText
    }

    fun signOut() {
        viewModelScope.launch {
            portalItemRepository.deleteAll()
            portalSettings.signOut()
            // add a artificial delay before navigating screens
            delay(500)
            navigator.navigateTo(NavigationRoute.Login)
        }
    }
}
