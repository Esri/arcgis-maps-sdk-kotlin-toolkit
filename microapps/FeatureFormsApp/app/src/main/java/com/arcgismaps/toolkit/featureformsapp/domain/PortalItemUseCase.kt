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
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

data class PortalItemData(
    val portalItem: PortalItem,
    val formLayerName: String? = null
)

/**
 * A domain layer to transform item data into loaded PortalItems, along with some domain knowledge of what layer
 * in each item is of interest.
 */
class PortalItemUseCase(
    scope: CoroutineScope,
    private val itemRepository: ItemRepository,
    private val portalItemRepository: PortalItemRepository
) {

    private val itemsMap: MutableMap<String, PortalItemData> = mutableMapOf()

    private val portalItemsFlow: MutableStateFlow<List<PortalItemData>> =
        MutableStateFlow(emptyList())

    init {
        scope.launch {
            itemRepository.observe().collect { itemDataList ->
                Log.e("TAG", "got : $itemDataList: ")
                val time = measureTimeMillis {
                    itemDataList.forEach { itemData ->
                        if (itemData.url !in itemsMap) {

                            val portalItem =
                                portalItemRepository.getEntry(itemData.url)?.let { cacheEntry ->
                                    Log.e("TAG", "cache: using from cache", )
                                    val portal = Portal(cacheEntry.portalUrl)
                                    PortalItem.fromJsonOrNull(cacheEntry.json, portal)
                                } ?: run {
                                    Log.e("TAG", "cache: no cache item found", )
                                    PortalItem(itemData.url).also {
                                        it.load()
                                        portalItemRepository.createEntry(
                                            itemData.url,
                                            it.toJson(),
                                            it.portal.url
                                        )
                                    }
                                }

                            val portalItemData = PortalItemData(
                                portalItem = portalItem,
                                formLayerName = formLayerName(itemData)
                            )

                            itemsMap[itemData.url] = portalItemData
                        }
                    }
                    portalItemsFlow.value = itemsMap.values.toList()
                }
                Log.e("TAG", "total portal load time: $time")
            }
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

    fun observe(): Flow<List<PortalItemData>> = portalItemsFlow

    suspend fun refresh() = itemRepository.refresh()

    /**
     * Used by the UI to get a specific PortalItem by url
     */
    operator fun invoke(url: String): PortalItemData? = itemsMap[url]
}
