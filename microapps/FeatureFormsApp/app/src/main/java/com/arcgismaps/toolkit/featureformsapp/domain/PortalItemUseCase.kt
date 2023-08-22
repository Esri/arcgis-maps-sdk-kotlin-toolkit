/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureformsapp.domain

import android.util.Log
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.data.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class PortalItemData(
    val portalItem: PortalItem,
    val itemData: ItemData,
    val formLayerName: String? = null
)

/**
 * A domain layer to transform item data into loaded PortalItems, along with some domain knowledge of what layer
 * in each item is of interest.
 */
class PortalItemUseCase(
    scope: CoroutineScope,
    private val itemRepository: ItemRepository
) {

    private val itemsFlow: MutableStateFlow<List<PortalItemData>> = MutableStateFlow(emptyList())

    init {
        scope.launch {
            itemRepository.observe().collect { list ->
                val portalItemData = list.map { itemData ->
                    PortalItemData(
                        portalItem = PortalItem(itemData.url),
                        itemData = itemData,
                        formLayerName = formLayerName(itemData)
                    ).also {
                        it.portalItem.load()
                        it.portalItem.thumbnail?.load()
                        Log.e("TAG", "loaded portal ${it.portalItem.itemId} ", )
                    }
                }
                itemsFlow.emit(portalItemData)
            }
        }
        scope.launch {
            itemRepository.refresh()
        }
    }

    /**
     * Provide the name of the layer with forms that we would like to use to identify features. If not needed, because
     * the map has one layer for example, then return null.
     */
    private fun formLayerName(itemData: ItemData): String? =
        if (itemData.url == "https://runtimecoretest.maps.arcgis.com/home/item.html?id=0f6864ddc35241649e5ad2ee61a3abe4") {
            "CityworksDynamic - Water Hydrants"
        } else {
            null
        }

    fun observe(): Flow<List<PortalItemData>> = itemsFlow

    /**
     * Used by the UI to get a specific PortalItem by url
     */
    operator fun invoke(url: String): PortalItemData = itemsFlow.value.first {
        url == it.portalItem.url
    }
}
