package com.arcgismaps.toolkit.featureformsapp.data

import android.graphics.Bitmap
import android.os.FileUtils
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
import kotlin.system.measureTimeMillis

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
    private val scope: CoroutineScope,
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
            val remoteItems: List<ItemData>
            // get network items
            val fetchTime = measureTimeMillis {
                remoteItems = remoteDataSource.fetchItemData(portalUri, connection)
            }
            Log.e("TAG", "refresh: got remote items $fetchTime")
            // load the portal items and add them to cache
            val cacheTime = measureTimeMillis {
                // call your function here
                loadAndCachePortalItems(localItems + remoteItems)
            }
            Log.e("TAG", "refresh: caching complete in $cacheTime")
        }
    }

    suspend fun deleteAll() = withContext(dispatcher) {
        mutex.withLock {
            deleteAllCacheEntries()
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
        val portalItems = items.map { PortalItem(it.url) }
        portalItems.map { portalItem ->
            launch {
                portalItem.load().onFailure {
                    Log.e("PortalItemRepository", "loadAndCachePortalItems: $it")
                }
                portalItem.thumbnail?.let { thumbnail ->
                    thumbnail.load()
                    thumbnail.image?.bitmap?.let { bitmap ->
                        createThumbnail(portalItem.itemId, bitmap)
                    }
                }
            }
        }.joinAll()

        val entries = portalItems.mapNotNull { portalItem ->
            // ignore if the portal items fails to load
            if (portalItem.loadStatus.value is LoadStatus.FailedToLoad || portalItem.loadStatus.value is LoadStatus.NotLoaded) {
                null
            } else {
                ItemCacheEntry(
                    portalItem.itemId,
                    portalItem.toJson(),
                    getThumbnailUri(portalItem.itemId),
                    portalItem.portal.url
                )
            }
        }
        // add all the items into the local cache storage
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
    private suspend fun deleteAllCacheEntries() = withContext(Dispatchers.IO) {
        itemCacheDao.deleteAll()
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

    /**
     * Returns the thumbnail file path for a portal item with the [itemId]. An empty string
     * is returned if a thumbnail does not exist.
     */
    private suspend fun getThumbnailUri(itemId: String): String = withContext(Dispatchers.IO) {
        val file = File("${thumbsDirPath}/${itemId}.jpg")
        return@withContext if (file.exists()) {
            file.absolutePath
        } else ""
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
