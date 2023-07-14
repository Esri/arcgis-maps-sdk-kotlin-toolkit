package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel class which acts as the data source of PortalItems to load.
 */
class MapListViewModel : ViewModel() {

    // private backing property for portalItems as a mutable list
    private val _portalItems : MutableList<PortalItem> = mutableListOf()
    // the list of loaded portal items
    val portalItems: List<PortalItem> = _portalItems
    // state flow that indicates if the data is being loaded
    val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            // load the portal items
            loadPortalItems()
            // emit false to indicate loading is done
            isLoading.value = false
        }
    }

    /**
     * Loads portal items from [getListOfMaps].
     */
    private suspend fun loadPortalItems() {
        for (map in getListOfMaps()) {
            val portalItem = PortalItem(map)
            // load the actual portalItem
            portalItem.load()
            // load its thumbnail
            portalItem.thumbnail?.load()
            _portalItems.add(portalItem)
        }
    }
}

/**
 * Data source of a list of portal urls
 */
fun getListOfMaps(): List<String> {
    return listOf(
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=0f6864ddc35241649e5ad2ee61a3abe4",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=df0f27f83eee41b0afe4b6216f80b541",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=454422bdf7e24fb0ba4ffe0a22f6bf37",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=c606b1f345044d71881f99d00583f8f7",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=622c4674d6f64114a1de2e0b8382fcf3",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=a81d90609e4549479d1f214f28335af2",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=bb4c5e81740e4e7296943988c78a7ea6",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=5d69e2301ad14ec8a73b568dfc29450a"
    )
}
