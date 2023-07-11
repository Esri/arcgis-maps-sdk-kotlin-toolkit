package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val portalItemUseCase: PortalItemUseCase
) : ViewModel() {
    private val _portalItems: MutableStateFlow<List<PortalItemData>> = MutableStateFlow(emptyList())
    val portalItems: StateFlow<List<PortalItemData>> = _portalItems.asStateFlow()
    
    init {
        viewModelScope.launch {
            _portalItems.value = portalItemUseCase.fetchPortalItemData()
        }
    }
}
