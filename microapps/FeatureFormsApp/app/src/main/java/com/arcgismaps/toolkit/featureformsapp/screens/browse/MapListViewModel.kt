package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemWithLayer
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val portalItemUseCase: PortalItemUseCase
) : ViewModel() {

    // State flow to keep track of current loading state
    private val _isLoading = MutableStateFlow(false)
    
    private val _searchText = MutableStateFlow("")
    
    // State flow that combines the _isLoading and the PortalItemUseCase data flow to create
    // a MapListUIState
    val uiState: StateFlow<MapListUIState> =
        combine(_isLoading, _searchText, portalItemUseCase.observe()) { isLoading, searchText, portalItemData ->
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
    }

    /**
     * Refreshes the data. [forceUpdate] clears the local cache.
     */
    fun refresh(forceUpdate: Boolean) {
        if (!_isLoading.value) {
            viewModelScope.launch {
                _isLoading.emit(true)
                portalItemUseCase.refresh(forceUpdate)
                _isLoading.emit(false)
            }
        }
    }
    
    fun filterPortalItems(filterText: String) {
        _searchText.value = filterText
    }
}
