package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemWithLayer
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
    val data: List<PortalItemWithLayer>
)

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
    private val portalItemUseCase: PortalItemUseCase,
    private val portalSettings: PortalSettings,
    private val navigator: Navigator
) : ViewModel() {

    private val authenticatorState = AuthenticatorState()

    // State flow to keep track of current loading state
    private val _isLoading = MutableStateFlow(false)

    private val _searchText = MutableStateFlow("")

    // State flow that combines the _isLoading and the PortalItemUseCase data flow to create
    // a MapListUIState
    val uiState: StateFlow<MapListUIState> =
        combine(
            _isLoading,
            _searchText,
            portalItemUseCase.observe()
        ) { isLoading, searchText, portalItemData ->
            val data = portalItemData.filter {
                searchText.isEmpty()
                    || it.data.portalItem.title.uppercase().contains(searchText.uppercase())
                    || it.data.portalItem.itemId.contains(searchText)
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
            if (portalItemUseCase.isEmpty()) {
                refresh(false)
            }
        }
        viewModelScope.launch {
            authenticatorState.pendingServerTrustChallenge.collect {
                it?.trust()
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
     * Refreshes the data. [forceUpdate] clears the local cache.
     */
    fun refresh(forceUpdate: Boolean) {
        if (!_isLoading.value) {
            viewModelScope.launch {
                _isLoading.emit(true)
                portalItemUseCase.refresh(
                    portalSettings.getPortalUrl(),
                    portalSettings.getPortalConnection(),
                    forceUpdate
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
            portalItemUseCase.deleteAll()
            portalSettings.signOut()
            delay(1000)
            navigator.navigateTo(NavigationRoute.Login)
        }
    }
}
