package com.arcgismaps.toolkit.featureformsapp.screens.browse

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.portal.LoadableImage
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
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

    private val _isLoading = MutableStateFlow(false)

    val imageLoader: suspend (LoadableImage) -> Unit = {
        viewModelScope.launch(Dispatchers.IO) { it.load() }.join()
    }

    val uiState: StateFlow<MapListUIState> =
        combine(_isLoading, portalItemUseCase.observe()) { isLoading, portalItemData ->
            MapListUIState(isLoading, portalItemData)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),
            initialValue = MapListUIState(true, emptyList())
        )

    init {
        viewModelScope.launch {
            if (portalItemUseCase.isEmpty()) {
                refresh()
            }
        }
    }

    fun refresh() {
        if (!_isLoading.value) {
            viewModelScope.launch {
                _isLoading.emit(true)
                portalItemUseCase.refresh()
                _isLoading.emit(false)
            }
        }
    }
}
