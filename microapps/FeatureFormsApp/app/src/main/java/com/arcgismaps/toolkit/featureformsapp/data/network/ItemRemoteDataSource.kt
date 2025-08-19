/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureformsapp.data.network

import android.util.Log
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.portal.PortalQueryParameters
import com.arcgismaps.portal.PortalQueryResultSet
import com.arcgismaps.portal.PortalUser
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class to hold the user content fetched from the portal.
 */
class UserContent(
    val items: List<PortalItem>,
    val folders: List<PortalFolder>
)

/**
 * Main data source for accessing the portal item data from the network.
 */
class ItemRemoteDataSource(
    private val dispatcher: CoroutineDispatcher,
    private val portalSettings: PortalSettings,
    coroutineScope: CoroutineScope
) {
    private var portalUser : PortalUser? = null

    init {
        coroutineScope.launch {
            portalSettings.user.collectLatest { user ->
                portalUser = user
            }
        }
    }

    suspend fun fetchContent(): UserContent = withContext(dispatcher) {
        portalUser?.let { user ->
            // log an exception and return if the portal user loading fails
            user.load().onFailure {
                Log.e("ItemRemoteDataSource", "unable to load the portal user: ${it.message}", it)
                return@withContext emptyUserContent()
            }
            // fetch the users content
            val portalUserContent =
                user.fetchContent().getOrElse { return@withContext emptyUserContent() }
            val items = portalUserContent.items.filter {
                it.type == PortalItemType.WebMap
            }
            return@withContext UserContent(
                items = items,
                folders = portalUserContent.folders
            )
        } ?: return@withContext emptyUserContent()
    }

    suspend fun fetchItemsInFolder(folderId: String): List<PortalItem> = withContext(dispatcher) {
        portalUser?.let { user ->
            // log an exception and return if the portal user loading fails
            user.load().onFailure {
                Log.e("ItemRemoteDataSource", "unable to load the portal user: ${it.message}", it)
                return@withContext emptyList()
            }
            // fetch the users content
            val items = user.fetchContentInFolder(folderId).getOrDefault(emptyList()).filter {
                it.type == PortalItemType.WebMap
            }
            return@withContext items
        } ?: return@withContext emptyList()
    }

    suspend fun searchContent(query: PortalQueryParameters): Result<PortalQueryResultSet<PortalItem>> =
        withContext(dispatcher) {
            portalUser?.let { user ->
                // log an exception and return if the portal user loading fails
                user.load().onFailure {
                    Log.e("ItemRemoteDataSource", "unable to load the portal user: ${it.message}", it)
                    return@withContext Result.failure(it)
                }
                return@withContext user.portal.findItems(query)
            } ?: return@withContext Result.failure(
                IllegalStateException("Portal User is not initialized")
            )
        }

    private fun emptyUserContent(): UserContent {
        return UserContent(
            items = emptyList(),
            folders = emptyList()
        )
    }
}
