package com.arcgismaps.toolkit.featureformsapp.data.network

import android.util.Log
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemApi
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Main data source for accessing the portal item data from the network.
 */
class ItemRemoteDataSource(
    private val dispatcher: CoroutineDispatcher,
    private val itemApi: ItemApi = object : ItemApi {
        override suspend fun fetchItems(portalUri : String, connection : Portal.Connection): List<ItemData> {
            // create a Portal
            val portal = Portal(
                portalUri,
                connection = connection
            )
            Log.e("TAG", "fetchItems: loading portal $portalUri", )
            // log an exception and return if the portal loading fails
            portal.load().onFailure {
                Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}")
                return emptyList()
            }.onSuccess {
                Log.e("TAG", "fetchItems: sucess", )
            }
            Log.e("TAG", "fetchItems: done loading", )
            val user = portal.user ?: return emptyList()
            // fetch the users content
            val portalUserContent = user.fetchContent().getOrElse { return emptyList() }
            Log.e("TAG", "fetchItems: got content ${portalUserContent.items.count()}", )
            // get the specified folder under the users content
            val folder = portalUserContent.folders.firstOrNull {
                it.title == portalFolder
            }
            return if (folder != null) {
                // fetch and return content within the specified folder
                user.fetchContentInFolder(folder.folderId).getOrDefault(emptyList()).filter {
                    // filter the content by WebMaps only
                    it.type == PortalItemType.WebMap
                }.map {
                    ItemData(it.url)
                }
            } else {
                portalUserContent.items.filter {
                    it.type == PortalItemType.WebMap
                }.map {
                    ItemData(it.url)
                }
            }
        }
    }
) {
    /**
     * The primary portal url.
     */
    //var portalUri = "https://www.arcgis.com"

    companion object {
        /**
         * Folder under the portal to fetch the portal items from.
         */
        const val portalFolder = "Apollo"
    }

    suspend fun fetchItemData(portalUri : String, connection : Portal.Connection): List<ItemData> = withContext(dispatcher) {
        itemApi.fetchItems(portalUri, connection)
    }
}
