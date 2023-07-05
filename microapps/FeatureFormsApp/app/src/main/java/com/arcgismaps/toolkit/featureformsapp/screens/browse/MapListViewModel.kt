package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.PortalItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MapListViewModel : ViewModel() {

    private val _portalItems : MutableList<PortalItem> = mutableListOf()
    val portalItems: List<PortalItem> = _portalItems

    val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            loadPortalItems()
            isLoading.value = false
        }
    }

    private suspend fun loadPortalItems() {
        for (map in getListOfMaps()) {
            val portalItem = PortalItem(map)
            portalItem.load()
            portalItem.thumbnail?.load()
            _portalItems.add(portalItem)
        }
    }
}

fun getListOfMaps(): List<String> {
    return listOf(
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=df0f27f83eee41b0afe4b6216f80b541",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=454422bdf7e24fb0ba4ffe0a22f6bf37",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=c606b1f345044d71881f99d00583f8f7",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=622c4674d6f64114a1de2e0b8382fcf3",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=a81d90609e4549479d1f214f28335af2",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=bb4c5e81740e4e7296943988c78a7ea6",
        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=5d69e2301ad14ec8a73b568dfc29450a"
    )
}
