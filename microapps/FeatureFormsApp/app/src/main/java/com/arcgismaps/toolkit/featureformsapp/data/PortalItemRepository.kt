package com.arcgismaps.toolkit.featureformsapp.data

import android.graphics.Bitmap
import android.util.Log
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheEntry
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
import java.io.File
import java.io.FileOutputStream

data class PortalItemData(
    val portalItem: PortalItem,
    var thumbnailUri: String
)

/**
 * A repository to map the data source items into loaded PortalItems. This is the primary repository
 * to interact with by the UI/domain layer. This also uses the [ItemCacheDao] to provide a caching
 * mechanism.
 */
class PortalItemRepository(
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val remoteDataSource: ItemRemoteDataSource,
    private val itemCacheDao: ItemCacheDao,
    private val filesDir: String
) {
    // in memory cache of loaded portal items
    private val portalItems: MutableMap<String, PortalItem> = mutableMapOf()

    // to protect shared state of portalItems
    private val mutex = Mutex()

    private lateinit var thumbsDirPath: String

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

    init {
        // create the thumbnails directory if it does not exist
        // and save its absolute path
        scope.launch(Dispatchers.IO) {
            val thumbsDir = File("$filesDir/thumbs")
            if (!thumbsDir.exists()) thumbsDir.mkdirs()
            thumbsDirPath = thumbsDir.absolutePath
        }
    }

    /**
     * Returns the list of loaded PortalItemData as a flow.
     */
    fun observe(): Flow<List<PortalItemData>> = portalItemsFlow

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
        deleteAll()
        mutex.withLock {
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
        val portalItemData = items.map {
            PortalItemData(PortalItem(it.url), "")
        }
        portalItemData.map { data ->
            // load each portal item and its thumbnail in a new coroutine
            launch {
                data.portalItem.load().onFailure {
                    Log.e("PortalItemRepository", "loadAndCachePortalItems: $it")
                }
                data.portalItem.thumbnail?.let { thumbnail ->
                    thumbnail.load()
                    thumbnail.image?.bitmap?.let { bitmap ->
                        data.thumbnailUri = createThumbnail(data.portalItem.itemId, bitmap)
                    }
                }
            }
            // suspend till all the portal loading jobs are complete
        }.joinAll()
        // create entries to be inserted into the local cache storage.
        val entries = portalItemData.mapNotNull { data ->
            // ignore if the portal item fails to load
            if (data.portalItem.loadStatus.value is LoadStatus.FailedToLoad) {
                null
            } else {
                ItemCacheEntry(
                    itemId = data.portalItem.itemId,
                    json = data.portalItem.toJson(),
                    thumbnailUri = data.thumbnailUri,
                    portalUrl = data.portalItem.portal.url
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

    /**
     * Creates a JPEG thumbnail using the [bitmap] with [itemId].jpg filename in the local files
     * directory and returns the absolute path to the file.
     */
    private suspend fun createThumbnail(itemId: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val file = File("${thumbsDirPath}/${itemId}.jpg")
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
        "https://www.arcgis.com/home/item.html?id=a95963333bf84055b7115dc60d10443e"
    )
