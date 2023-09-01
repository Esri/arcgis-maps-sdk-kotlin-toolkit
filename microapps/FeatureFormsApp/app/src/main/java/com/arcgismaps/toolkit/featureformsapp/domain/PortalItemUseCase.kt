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

import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.data.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PortalItemData(val portalItem: PortalItem, val formLayerName: String? = null)

/**
 * A domain layer to transform item data into loaded PortalItems, along with some domain knowledge of what layer
 * in each item is of interest.
 */
class PortalItemUseCase(private val scope: CoroutineScope, private val itemRepository: ItemRepository) {
    private val mutex = Mutex()
    
    private var items: List<PortalItemData> = emptyList()
    
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
    
    /**
     * Fetch item data, transform it into loaded PortalItems, and cache them.
     */
    suspend fun fetchPortalItemData(): List<PortalItemData> {
        return if (items.isEmpty()) {
            withContext(scope.coroutineContext) {
                itemRepository.fetchItems().map { itemData ->
                    PortalItemData(PortalItem(itemData.url), formLayerName(itemData)).also { portalItemData ->
                        portalItemData.portalItem.load()
                            .onSuccess {
                                val portalItem = portalItemData.portalItem
                                portalItem.thumbnail?.load()
                            }
                    }
                }.also {
                    mutex.withLock { items = it }
                }
            }
        } else {
            mutex.withLock { items }
        }
    }
    
    /**
     * Used by the UI to get a specific PortalItem by url
     */
    suspend operator fun invoke(url: String): PortalItemData =
        fetchPortalItemData().first {
            url == it.portalItem.url
        }
}
