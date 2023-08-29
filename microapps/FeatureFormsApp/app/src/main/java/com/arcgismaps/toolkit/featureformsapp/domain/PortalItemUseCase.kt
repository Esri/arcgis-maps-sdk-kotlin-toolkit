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
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

data class PortalItemData(
    val portalItem: PortalItem,
    val formLayerName: String? = null
)

/**
 * A domain layer to transform the loaded portal items with some added domain knowledge of what layer
 * in each item is of interest.
 */
class PortalItemUseCase(
    dispatcher: CoroutineDispatcher,
    scope: CoroutineScope,
    private val portalItemRepository: PortalItemRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemDataFlow: Flow<List<PortalItemData>> = portalItemRepository.getItems().mapLatest {
        it.map { item ->
            PortalItemData(
                portalItem = item,
                formLayerName = formLayerName(item.itemId)
            )
        }
    }.flowOn(dispatcher)

    fun observe(): Flow<List<PortalItemData>> = portalItemDataFlow

    suspend fun refresh(forceUpdate : Boolean) = portalItemRepository.refresh(forceUpdate)

    suspend fun isEmpty(): Boolean = portalItemRepository.getItemCount() == 0

    /**
     * Provide the name of the layer with forms that we would like to use to identify features. If not needed, because
     * the map has one layer for example, then return null.
     */
    private fun formLayerName(itemId: String): String? =
        if (itemId == "0f6864ddc35241649e5ad2ee61a3abe4") {
            "CityworksDynamic - Water Hydrants"
        } else {
            null
        }

    /**
     * Used by the UI to get a specific PortalItem by url
     */
    operator fun invoke(itemId: String): PortalItemData? {
        return portalItemRepository(itemId)?.let {
            PortalItemData(it, formLayerName(it.itemId))
        }
    }
}
