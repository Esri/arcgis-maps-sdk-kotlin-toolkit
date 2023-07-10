package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
class MapListViewModel(private val portalItemUseCase: PortalItemUseCase) : ViewModel() {
    
    lateinit var portalItems: List<PortalItemData>
    
    // state flow that indicates if the data is being loaded
    val isLoading = MutableStateFlow(true)
    
    init {
        viewModelScope.launch {
            portalItems = portalItemUseCase.fetchPortalItemData()
            // emit false to indicate loading is done
            isLoading.value = false
            
        }
    }
}

/**
 * Factory for the [MapViewModel]
 */
class MapListViewModelFactory(
    private val portalItemUseCase: PortalItemUseCase
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapListViewModel(portalItemUseCase) as T
    }
}

