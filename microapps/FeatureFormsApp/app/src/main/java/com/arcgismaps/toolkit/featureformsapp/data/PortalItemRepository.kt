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

package com.arcgismaps.toolkit.featureformsapp.data

import android.util.Log
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A repository to map the data source items into loaded PortalItems. This is the primary repository
 * to interact with by the UI/domain layer. This also uses the [ItemCacheDao] to provide a caching
 * mechanism.
 */
class PortalItemRepository(
    private val dispatcher: CoroutineDispatcher,
    private val remoteDataSource: ItemRemoteDataSource,
    private val itemCacheDao: ItemCacheDao,
    private val filesDir: String
) {
    // in memory cache of loaded portal items
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    // to protect shared state of portalItems
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemsFlow: Flow<List<PortalItem>> =
        itemCacheDao.observeAll().mapLatest { entries ->
            // map the cache entries into loaded portal items
            entries.mapNotNull { entry ->
                val portal = Portal(entry.portalUrl)
                val portalItem = PortalItem.fromJsonOrNull(entry.json, portal)
                portalItem?.let {
                    portalItem.also {
                        portalItems[portalItem.itemId] = it
                    }
                }
            }
        }.flowOn(dispatcher)

    /**
     * Returns the list of loaded PortalItemData as a flow.
     */
    fun observe(): Flow<List<PortalItem>> = portalItemsFlow

    /**
     * Refreshes the underlying data source to fetch the latest content.
     *
     * This operation is suspending and will wait until the underlying data source has finished
     * AND the repository has finished loading the portal items.
     */
    suspend fun refresh(
        portalUri: String,
        connection: Portal.Connection
    ) = withContext(dispatcher) {
        mutex.withLock {
            // delete existing cache items
            itemCacheDao.deleteAll()
            portalItems.clear()
            // get local items
            val localItems = getListOfMaps().map { ItemData(it) }
            // get network items
            val remoteItems = remoteDataSource.fetchItemData(portalUri, connection)
            // load the portal items and add them to cache
            loadAndCachePortalItems(localItems + remoteItems)
        }
    }

    /**
     * Deletes all the portal items from the local cache storage.
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            itemCacheDao.deleteAll()
            portalItems.clear()
        }
    }

    /**
     * Returns the number of items in the repository.
     */
    suspend fun getItemCount(): Int = withContext(dispatcher) {
        itemCacheDao.getCount()
    }

    /**
     * Loads the list of [items] into loaded portal items and adds them to the Cache.
     */
    private suspend fun loadAndCachePortalItems(items: List<ItemData>) = withContext(dispatcher) {
        // create PortalItems from the urls
        val portalItems = items.map {
            PortalItem(it.url)
        }
        portalItems.map { item ->
            // load each portal item and its thumbnail in a new coroutine
            launch {
                item.load().onFailure {
                    Log.e("PortalItemRepository", "loadAndCachePortalItems: $it")
                }
                item.thumbnail?.load()
            }
            // suspend till all the portal loading jobs are complete
        }.joinAll()
        // create entries to be inserted into the local cache storage.
        val entries = portalItems.mapNotNull { item ->
            // ignore if the portal item failed to load
            if (item.loadStatus.value is LoadStatus.FailedToLoad) {
                null
            } else {
                ItemCacheEntry(
                    itemId = item.itemId,
                    json = item.toJson(),
                    portalUrl = item.portal.url
                )
            }
        }
        // insert all the items into the local cache storage
        insertCacheEntries(entries)
    }

    /**
     * Deletes and inserts the list of [entries] using the [ItemCacheDao].
     */
    private suspend fun insertCacheEntries(entries: List<ItemCacheEntry>) =
        withContext(dispatcher) {
            itemCacheDao.deleteAndInsert(entries)
        }

    operator fun invoke(itemId: String): PortalItem? = portalItems[itemId]
}

/**
 * Local data source of a list of portal urls
 */
fun getListOfMaps(): List<String> =
    listOf(
        "https://www.arcgis.com/home/item.html?id=f72207ac170a40d8992b7a3507b44fad"
    )
