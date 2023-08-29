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

import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * A repository to sit between the data source and the UI/domain layer.
 */
class ItemRepository(
    private val dispatcher: CoroutineDispatcher,
    private val localDataSource: ItemDao,
    private val remoteDataSource: ItemRemoteDataSource
) {

    /**
     * Provides a Flow<List<ItemData>> to observe.
     */
    fun observe(): Flow<List<ItemData>> = localDataSource.observeAll().flowOn(dispatcher)

    /**
     * Returns the number of items in the current data source.
     */
    suspend fun getCount(): Int = withContext(dispatcher) { localDataSource.getCount() }

    /**
     * Refreshes the local and network data source to fetch the latest content. This operation is
     * suspending.
     */
    suspend fun refresh() = withContext(dispatcher) {
        // get local items
        val localItems = refreshLocalItems()
        // get network items
        val remoteItems = remoteDataSource.fetchItemData()
        // purge existing items and add the updated items
        localDataSource.deleteAndInsert(localItems + remoteItems)
    }

    private fun refreshLocalItems(): List<ItemData> = getListOfMaps().map { ItemData(it) }
}

/**
 * Local data source of a list of portal urls
 */
fun getListOfMaps(): List<String> =
    listOf(
        "https://www.arcgis.com/home/item.html?id=a95963333bf84055b7115dc60d10443e",
    )

