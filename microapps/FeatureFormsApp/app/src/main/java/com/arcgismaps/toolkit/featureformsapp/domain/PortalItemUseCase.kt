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

import com.arcgismaps.toolkit.featureformsapp.data.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

data class PortalItemWithLayer(val data: PortalItemData, val formLayerName: String? = null)


/**
 * A domain layer to transform the loaded portal items with some added domain knowledge of what layer
 * in each item is of interest.
 */
class PortalItemUseCase(
    dispatcher: CoroutineDispatcher,
    private val portalItemRepository: PortalItemRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemDataFlow: Flow<List<PortalItemWithLayer>> =
        portalItemRepository.observe().mapLatest {
            it.map { item ->
                PortalItemWithLayer(
                    data = item,
                    formLayerName = formLayerName(item.portalItem.itemId)
                )
            }
        }.flowOn(dispatcher)

    fun observe(): Flow<List<PortalItemWithLayer>> = portalItemDataFlow

    suspend fun refresh(forceUpdate: Boolean) = portalItemRepository.refresh(forceUpdate)

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
    operator fun invoke(itemId: String): PortalItemWithLayer? {
        return portalItemRepository(itemId)?.let {
            PortalItemWithLayer(PortalItemData(it, ""), formLayerName(it.itemId))
        }
    }
}
