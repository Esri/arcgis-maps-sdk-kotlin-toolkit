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

package com.arcgismaps.toolkit.featureformsapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ItemRepository(private val itemDataSource: ItemDataSource, private val scope: CoroutineScope) {
    private val mutex = Mutex()
    
    private var itemData: List<ItemData> = emptyList()
    
    suspend fun fetchItems(): List<ItemData> {
        return if (itemData.isEmpty()) {
            withContext(scope.coroutineContext) {
                itemDataSource.fetchItemData().also { list ->
                    mutex.withLock {
                        itemData = list
                    }
                }
            }
        } else {
            mutex.withLock { itemData }
        }
    }
}