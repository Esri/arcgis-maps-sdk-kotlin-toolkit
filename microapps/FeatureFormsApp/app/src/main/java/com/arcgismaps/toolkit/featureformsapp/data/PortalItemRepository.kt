package com.arcgismaps.toolkit.featureformsapp.data

import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/**
 * A repository to map the data source items into loaded PortalItems. This is the primary repository
 * to interact with by the UI/domain layer. This also uses the [ItemCacheDao] to provide a caching
 * mechanism.
 */
class PortalItemRepository(
    private val dispatcher: CoroutineDispatcher,
    private val itemRepository: ItemRepository,
    private val itemCacheDao: ItemCacheDao,
) {
    // in memory cache of loaded portal items
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemFlow: Flow<List<PortalItem>> =
        itemRepository.observe().mapLatest {
            it.map { itemData ->
                // check if a db cache entry exists for this portal item
                val portalItem = getCacheEntry(itemData.url)?.let { cacheEntry ->
                    val portal = Portal(cacheEntry.portalUrl)
                    PortalItem.fromJsonOrNull(cacheEntry.json, portal)
                    // if none exists, create one and add it to the db cache
                } ?: PortalItem(itemData.url).also { portalItem ->
                    portalItem.load()
                    createCacheEntry(
                        itemData.url,
                        portalItem.toJson(),
                        portalItem.portal.url
                    )
                }
                portalItems[portalItem.itemId] = portalItem
                portalItem.thumbnail?.load()
                portalItem
            }
        }.flowOn(dispatcher)

    /**
     * Returns the list of loaded portal items as a flow.
     */
    fun getItems(): Flow<List<PortalItem>> = portalItemFlow

    /**
     * Refreshes the underlying data source to fetch the latest content. [forceUpdate] when set to
     * true, will clear the existing cache.
     *
     * This operation is suspending and will wait until the underlying data source has finished
     * AND the repository and finished loading the portal items.
     */
    suspend fun refresh(forceUpdate: Boolean = false) = withContext(dispatcher) {
        if (forceUpdate) deleteAllCacheEntries()
        portalItems.clear()
        itemRepository.refresh()
        // wait until the portalItemFlow has emitted which indicates a completed load
        portalItemFlow.conflate().firstOrNull()
    }

    /**
     * Returns the number of items in the repository.
     */
    suspend fun getItemCount(): Int = withContext(dispatcher) {
        return@withContext itemRepository.getCount()
    }

    private suspend fun createCacheEntry(itemId: String, json: String, portalUrl: String) =
        withContext(dispatcher) {
            itemCacheDao.upsert(ItemCacheEntry(itemId, json, portalUrl))
        }

    private suspend fun getCacheEntry(itemId: String): ItemCacheEntry? = withContext(dispatcher) {
        return@withContext itemCacheDao.getById(itemId)
    }

    private suspend fun deleteAllCacheEntries() =
        withContext(dispatcher) { itemCacheDao.deleteAll() }

    operator fun invoke(itemId: String): PortalItem? = portalItems[itemId]
}
