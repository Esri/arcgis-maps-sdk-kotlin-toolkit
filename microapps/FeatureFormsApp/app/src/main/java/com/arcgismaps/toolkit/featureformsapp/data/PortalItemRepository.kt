package com.arcgismaps.toolkit.featureformsapp.data

import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class PortalItemRepository(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val itemRepository: ItemRepository,
    private val itemCacheDao: ItemCacheDao,
) {
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemFlow: Flow<List<PortalItem>> =
        itemRepository.observe().mapLatest {
            it.map { itemData ->
                val portalItem = getCacheEntry(itemData.url)?.let { cacheEntry ->
                    val portal = Portal(cacheEntry.portalUrl)
                    PortalItem.fromJsonOrNull(cacheEntry.json, portal)
                } ?: PortalItem(itemData.url).also { portalItem ->
                    portalItem.load()
                    createCacheEntry(
                        itemData.url,
                        portalItem.toJson(),
                        portalItem.portal.url
                    )
                }
                portalItems[itemData.url] = portalItem
                portalItem.thumbnail?.load()
                portalItem
            }
        }.flowOn(dispatcher)

    fun getItems(): Flow<List<PortalItem>> = portalItemFlow

    suspend fun refresh(forceUpdate: Boolean = false) = withContext(dispatcher) {
        if (forceUpdate) deleteAllCacheEntries()
        itemRepository.refresh()
        portalItemFlow.conflate().firstOrNull()
    }

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

    operator fun invoke(url: String): PortalItem? = portalItems[url]
}
