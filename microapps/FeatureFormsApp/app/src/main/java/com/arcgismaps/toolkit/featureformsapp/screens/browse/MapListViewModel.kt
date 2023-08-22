package com.arcgismaps.toolkit.featureformsapp.screens.browse

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.data.DataSourceType
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapListUIState (
    val localPortalItems : List<PortalItem> = emptyList(),
    val authenticatedPortalItems : List<PortalItem> = emptyList()
)

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
    private val portalItemUseCase: PortalItemUseCase
) : ViewModel() {

    private val _uiState : MutableStateFlow<MapListUIState> = MutableStateFlow(MapListUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            portalItemUseCase.observe().collect { list ->

                val items = list.partition {  portalItemData ->
                    portalItemData.itemData.type == DataSourceType.Local
                }

                val localItems = items.first.map { it.portalItem }
                val authenticatedItems = items.second.map { it.portalItem }

                Log.e("TAG", "local: $localItems", )
                Log.e("TAG", "Remote: $authenticatedItems", )
                _uiState.emit(MapListUIState(localItems, authenticatedItems))
            }
        }
    }
}
