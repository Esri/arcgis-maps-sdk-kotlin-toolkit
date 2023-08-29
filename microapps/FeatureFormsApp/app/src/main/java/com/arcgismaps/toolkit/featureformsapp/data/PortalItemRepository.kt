package com.arcgismaps.toolkit.featureformsapp.data

import android.graphics.Bitmap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PortalItemData(
    val portalItem: PortalItem,
    val thumbnailUri: String
)

/**
 * A repository to map the data source items into loaded PortalItems. This is the primary repository
 * to interact with by the UI/domain layer. This also uses the [ItemCacheDao] to provide a caching
 * mechanism.
 */
class PortalItemRepository(
    private val dispatcher: CoroutineDispatcher,
    private val itemRepository: ItemRepository,
    private val itemCacheDao: ItemCacheDao,
    private val filesDir: String
) {
    // in memory cache of loaded portal items
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemFlow: Flow<List<PortalItemData>> =
        itemRepository.observe().mapLatest {
            it.map { itemData ->
                // check if a db cache entry exists for this portal item
                val cachedPortalItem = getCacheEntry(itemData.url)?.let { cacheEntry ->
                    val portal = Portal(cacheEntry.portalUrl)
                    PortalItem.fromJsonOrNull(cacheEntry.json, portal)?.let { portalItem ->
                        PortalItemData(portalItem, cacheEntry.thumbnailUri)
                    }
                }
                // if none exists, create one and add it to the db cache
                val portalItemData = if (cachedPortalItem == null) {
                    val portalItem = PortalItem(itemData.url)
                    portalItem.load()
                    val thumbnailUri = portalItem.thumbnail?.let { thumbnail ->
                        thumbnail.load()
                        thumbnail.image?.bitmap?.let { bitmap ->
                            createThumbnail(
                                portalItem.itemId,
                                bitmap
                            )
                        }
                    } ?: ""
                    createCacheEntry(
                        itemData.url,
                        portalItem.toJson(),
                        thumbnailUri,
                        portalItem.portal.url
                    )
                    PortalItemData(portalItem, thumbnailUri)
                } else cachedPortalItem
                portalItems[itemData.url] = portalItemData.portalItem
                portalItemData
            }
        }.flowOn(dispatcher)

    /**
     * Returns the list of loaded portal items as a flow.
     */
    fun getItems(): Flow<List<PortalItemData>> = portalItemFlow

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

    private suspend fun createCacheEntry(
        itemId: String,
        json: String,
        thumbnailUri: String,
        portalUrl: String
    ) = withContext(dispatcher) {
        itemCacheDao.upsert(ItemCacheEntry(itemId, json, thumbnailUri, portalUrl))
    }

    private suspend fun getCacheEntry(itemId: String): ItemCacheEntry? = withContext(dispatcher) {
        return@withContext itemCacheDao.getById(itemId)
    }

    private suspend fun deleteAllCacheEntries() =
        withContext(Dispatchers.IO) {
            itemCacheDao.deleteAll()
            val thumbsDir = File("$filesDir/thumbs")
            if (thumbsDir.exists()) thumbsDir.deleteRecursively()
        }

    private suspend fun createThumbnail(name: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val thumbsDir = File("$filesDir/thumbs")
            if (!thumbsDir.exists()) thumbsDir.mkdir()
            val file = File("${thumbsDir.absolutePath}/${name}.jpg")
            file.createNewFile()
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            return@withContext file.absolutePath
        }

    operator fun invoke(url: String): PortalItem? = portalItems[url]
}
