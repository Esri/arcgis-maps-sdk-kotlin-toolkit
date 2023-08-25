package com.arcgismaps.toolkit.featureformsapp.screens.browse

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapListUIState(
    val isLoading: Boolean,
    val data: List<PortalItemData>
)

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
    private val portalItemUseCase: PortalItemUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<MapListUIState> =
        MutableStateFlow(MapListUIState(false, emptyList()))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (portalItemUseCase.isEmpty()) {
                refresh()
            }
            portalItemUseCase.observe().collectLatest {
                Log.e("TAG", "a: rec", )
                _uiState.emit(
                    MapListUIState(isLoading = false, data = it)
                )
            }
        }
    }

    fun refresh() {
        if (!_uiState.value.isLoading) {
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Log.e("TAG", "refresh: refreshin", )
                _uiState.emit(MapListUIState(isLoading = true, emptyList()))
                val refreshedItems = portalItemUseCase.refresh()
                Log.e("TAG", "refresh: $refreshedItems", )
                if (refreshedItems == 0) {
                    _uiState.emit(
                        MapListUIState(isLoading = false, data = _uiState.value.data)
                    )
                }
            }
        }
    }
}
