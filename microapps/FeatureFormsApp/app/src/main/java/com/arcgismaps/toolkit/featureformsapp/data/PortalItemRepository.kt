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
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.portal.PortalQueryParameters
import com.arcgismaps.portal.PortalQueryResultSet
import com.arcgismaps.toolkit.featureformsapp.data.local.FolderCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.FolderCacheEntry
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
    private val folderCacheDao: FolderCacheDao,
    private val portalSettings: PortalSettings
) {
    // in memory cache of loaded portal items
    var activePortalItem : PortalItem? = null
        private set

    // to protect shared state of portalItems
    private val mutex = Mutex()

    fun setActivePortalItem(item: PortalItem?) {
        activePortalItem = item
    }

    /**
     * Returns the list of loaded PortalItemData as a flow.
     *
     * @param folder The folder to filter the portal items by. If null, only the items not in any
     * folder like the root folder will be returned.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(folder: String?): Flow<List<PortalItem>> =
        itemCacheDao.observeByFolder(folder).mapLatest { entries ->
            // map the cache entries into loaded portal items
            entries.mapNotNull { entry ->
                val portal = Portal(entry.portalUrl)
                PortalItem.fromJsonOrNull(entry.json, portal)
            }
        }.flowOn(dispatcher)

    /**
     * Returns the list of [FolderCacheEntry] as a flow.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFolders(): Flow<List<PortalFolder>> =
        folderCacheDao.observeAll().mapLatest {
            // map the cache entries into PortalFolder
            it.map { entry ->
                entry.toPortalFolder()
            }
        }.flowOn(dispatcher)

    /**
     * Refreshes the underlying data source to fetch the latest content.
     *
     * This operation is suspending and will wait until the underlying data source has finished
     * AND the repository has finished loading the portal items.
     */
    suspend fun refresh() = withContext(dispatcher) {
        mutex.withLock {
            // delete existing cache items
            itemCacheDao.deleteAll()
            activePortalItem = null
            val connection = portalSettings.getPortalConnection()
            val items = if (connection == Portal.Connection.Anonymous) {
                // if the connection is anonymous, only fetch the sample items
                getListOfMaps().map {
                    PortalItem(
                        portal = Portal.arcGISOnline(connection = connection),
                        itemId = it
                    )
                }
            } else {
                // if the connection is authenticated, fetch the content from the remote data source
                val content = remoteDataSource.fetchContent()
                // insert the folders into the local cache
                insertFolderEntries(content.folders)
                content.items
            }
            // load the portal items and add them to cache
            loadAndInsertPortalItems(items, deleteExisting = true)
        }
    }

    /**
     * Fetches the items in a specific [folder] from the remote data source and loads them into
     * the local cache storage. Use [observe] to observe the changes in the folder items.
     *
     * @param folder The [PortalFolder] to fetch items from.
     */
    suspend fun getItemsInFolder(folder: PortalFolder) = withContext(dispatcher) {
        val items = remoteDataSource.fetchItemsInFolder(folder.folderId)
        loadAndInsertPortalItems(items, deleteExisting = false)
    }

    /**
     * Returns the number of items in the repository for a specific [folder].
     */
    suspend fun getItemCount(folder: String?): Int = withContext(dispatcher) {
        itemCacheDao.getCount(folder)
    }

    /**
     * Returns the username of the currently signed-in user.
     */
    suspend fun getUsername(): String? {
        return portalSettings.user.firstOrNull()?.username
    }

    suspend fun searchItems(query: PortalQueryParameters): Result<PortalQueryResultSet<PortalItem>> {
        return remoteDataSource.searchContent(query)
    }

    /**
     * Signs out the user from the portal and clears the local cache.
     */
    suspend fun signOut() = withContext(dispatcher) {
        // clear the portal items and delete the cache
        mutex.withLock {
            itemCacheDao.deleteAll()
            folderCacheDao.deleteAll()
            activePortalItem = null
        }
        // sign out from the portal
        portalSettings.signOut()
    }

    /**
     * Loads the list of [portalItems] into loaded portal items and adds them to the Cache.
     *
     * @param portalItems The list of [PortalItem] to load and insert into the cache.
     * @param deleteExisting If true, deletes the existing items in the cache before inserting the new ones.
     */
    private suspend fun loadAndInsertPortalItems(
        portalItems: List<PortalItem>,
        deleteExisting: Boolean
    ) = withContext(dispatcher) {
        val entries = mutableListOf<ItemCacheEntry>()
        portalItems.map { item ->
            // load each portal item and its thumbnail in a new coroutine
            launch {
                item.load().onFailure {
                    Log.e("PortalItemRepository", "loadAndCachePortalItems: $it")
                }.onSuccess {
                    // create a cache entry for the item if it loaded successfully
                    entries.add(
                        ItemCacheEntry(
                            itemId = item.itemId,
                            json = item.toJson(),
                            portalUrl = item.portal.url,
                            parentFolderId = item.folderId
                        )
                    )
                }
            }
            // suspend till all the portal loading jobs are complete
        }.joinAll()
        // insert all the items into the local cache storage
        if (deleteExisting) {
            itemCacheDao.deleteAndInsert(entries)
        } else {
            itemCacheDao.insertAll(entries)
        }
    }

    private suspend fun insertFolderEntries(folders: List<PortalFolder>) = withContext(dispatcher) {
        folderCacheDao.deleteAndInsert(folders.map { it.toFolderCacheEntry() })
    }
}

/**
 * Local data source of a list of portal urls
 */
fun getListOfMaps(): List<String> =
    listOf(
        "f72207ac170a40d8992b7a3507b44fad"
    )

fun PortalFolder.toFolderCacheEntry(): FolderCacheEntry {
    return FolderCacheEntry(
        folderId = this.folderId,
        username = this.username,
        title = this.title,
        creationDate = this.creationDate
    )
}
