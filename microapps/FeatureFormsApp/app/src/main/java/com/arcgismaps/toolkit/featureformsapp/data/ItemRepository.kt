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

import android.util.Log
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A repository to sit between the data source and the UI/domain layer.
 */
class ItemRepository (
    private val scope: CoroutineScope,
    private val localDataSource: ItemDao,
    private val remoteDataSource: ItemRemoteDataSource
) {

    init {
        scope.launch {
            localDataSource.observeAll().collect {
                Log.e("TAG",  "got : $it: ", )
                itemData.emit(it)
            }
        }
    }

    private var itemData: MutableStateFlow<List<ItemData>> = MutableStateFlow(emptyList())

    fun observe(): Flow<List<ItemData>> = itemData.asStateFlow()

    suspend fun refresh() = withContext(scope.coroutineContext) {
        // get network items
        val remoteItems = remoteDataSource.fetchItemData()
        localDataSource.upsertAll(remoteItems)
    }
}
