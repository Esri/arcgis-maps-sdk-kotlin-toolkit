package com.arcgismaps.toolkit.featureformsapp.data

import android.graphics.Bitmap
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val remoteDataSource: ItemRemoteDataSource,
    private val itemCacheDao: ItemCacheDao,
    private val filesDir: String
) {
    // in memory cache of loaded portal items
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()
    // to protect shared state of portalItems
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val portalItemsFlow: Flow<List<PortalItemData>> =
        itemCacheDao.observeAll().mapLatest { entries ->
            // map the cache entries into loaded portal items
            entries.mapNotNull { entry ->
                val portal = Portal(entry.portalUrl)
                val portalItem = PortalItem.fromJsonOrNull(entry.json, portal)
                portalItem?.let {
                    portalItems[portalItem.itemId] = portalItem
                    PortalItemData(portalItem, entry.thumbnailUri)
                }
            }
        }.flowOn(dispatcher)

    /**
     * Returns the list of loaded PortalItemData as a flow.
     */
    fun observe(): Flow<List<PortalItemData>> = portalItemsFlow

    /**
     * Refreshes the underlying data source to fetch the latest content. [forceUpdate] when set to
     * true, will clear the existing cache.
     *
     * This operation is suspending and will wait until the underlying data source has finished
     * AND the repository has finished loading the portal items.
     */
    suspend fun refresh(forceUpdate: Boolean = false) = withContext(dispatcher) {
        mutex.withLock {
            if (forceUpdate) deleteAllCacheEntries()
            portalItems.clear()
            // get local items
            val localItems = getListOfMaps().map { ItemData(it) }
            // get network items
            val remoteItems = remoteDataSource.fetchItemData()
            // load the portal items and add them to cache
            loadAndCachePortalItems(localItems + remoteItems)
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
    private suspend fun loadAndCachePortalItems(items: List<ItemData>) {
        val entries = items.map { itemData ->
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
            ItemCacheEntry(
                itemData.url,
                portalItem.toJson(),
                thumbnailUri,
                portalItem.portal.url
            )
        }
        // purge existing items and add the updated items
        createCacheEntries(entries)
    }

    /**
     * Deletes and inserts the list of [entries] using the [ItemCacheDao].
     */
    private suspend fun createCacheEntries(entries: List<ItemCacheEntry>) =
        withContext(dispatcher) {
            itemCacheDao.deleteAndInsert(entries)
        }

    /**
     * Deletes all entries in the database using the [ItemCacheDao].
     */
    private suspend fun deleteAllCacheEntries() =
        withContext(Dispatchers.IO) {
            itemCacheDao.deleteAll()
            val thumbsDir = File("$filesDir/thumbs")
            if (thumbsDir.exists()) thumbsDir.deleteRecursively()
        }

    /**
     * Creates a JPEG thumbnail using the [bitmap] with [name] filename in the local files
     * directory and returns the absolute path to the file.
     */
    private suspend fun createThumbnail(name: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val thumbsDir = File("$filesDir/thumbs")
            if (!thumbsDir.exists()) thumbsDir.mkdirs()
            val file = File("${thumbsDir.absolutePath}/${name}.jpg")
            file.createNewFile()
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            file.absolutePath
        }

    operator fun invoke(itemId: String): PortalItem? = portalItems[itemId]
}

/**
 * Local data source of a list of portal urls
 */
fun getListOfMaps(): List<String> =
    listOf(
        "https://www.arcgis.com/home/item.html?id=a95963333bf84055b7115dc60d10443e",
        //"https://runtimecoretest.maps.arcgis.com/home/item.html?id=6d2d865bd6da48a79a7bb81f6a0176b9"
    )
