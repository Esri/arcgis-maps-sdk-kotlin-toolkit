package com.arcgismaps.toolkit.featureformsapp.data

import android.util.Log
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class PortalItemRepository(
    private val dispatcher: CoroutineDispatcher,
    private val itemRepository: ItemRepository,
    private val itemCacheDao: ItemCacheDao
) {
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemFlow: Flow<List<PortalItem>> = itemRepository.observe().mapLatest {
        it.map { itemData ->
            val portalItem = getEntry(itemData.url)?.let { cacheEntry ->
                Log.e("TAG", "cache: using from cache")
                val portal = Portal(cacheEntry.portalUrl)
                PortalItem.fromJsonOrNull(cacheEntry.json, portal)
            } ?: run {
                Log.e("TAG", "cache: no cache item found")
                PortalItem(itemData.url).also { portalItem ->
                    portalItem.load()
                    createEntry(
                        itemData.url,
                        portalItem.toJson(),
                        portalItem.portal.url
                    )
                }
            }
            portalItems[itemData.url] = portalItem
            portalItem
        }
    }.flowOn(dispatcher)

    fun getItems() : Flow<List<PortalItem>> = portalItemFlow

    suspend fun refresh() : List<Long> = withContext(dispatcher) {
        return@withContext itemRepository.refresh()
    }

    suspend fun getItemCount() : Int = withContext(dispatcher) {
        return@withContext itemRepository.getCount()
    }

    private suspend fun createEntry(itemId: String, json: String, portalUrl: String) =
        withContext(dispatcher) {
            itemCacheDao.upsert(ItemCacheEntry(itemId, json, portalUrl))
        }

    private suspend fun getEntry(itemId: String): ItemCacheEntry? = withContext(dispatcher) {
        return@withContext itemCacheDao.getById(itemId)
    }

    private suspend fun deleteAll() = withContext(dispatcher) {
        itemCacheDao.deleteAll()
    }

    operator fun invoke(url: String): PortalItem? = portalItems[url]
}
