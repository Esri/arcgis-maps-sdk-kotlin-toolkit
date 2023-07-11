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
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PortalItemData(val portalItem: PortalItem, val formLayerName: String)

class PortalItemUseCase(private val scope: CoroutineScope, private val itemRepository: ItemRepository) {
    private val mutex = Mutex()
    
    private var items: List<PortalItemData> = emptyList()
    
    suspend fun fetchPortalItemData(): List<PortalItemData> {
        return if (items.isEmpty()) {
            withContext(scope.coroutineContext) {
                itemRepository.fetchItems().map {
                    PortalItemData(PortalItem(it.itemId), it.formLayerName).also { itemData ->
                        itemData.portalItem.load()
                            .onSuccess {
                                itemData.portalItem.thumbnail?.load()
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
    
    suspend operator fun invoke(itemId: String): PortalItemData =
        fetchPortalItemData().first {
            it.portalItem.itemId == itemId
        }
}
