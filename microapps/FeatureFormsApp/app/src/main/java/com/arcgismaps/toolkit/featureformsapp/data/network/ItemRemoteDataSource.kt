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
    private val dispatcher: CoroutineDispatcher, private val itemApi: ItemApi = object : ItemApi {
        override suspend fun fetchItems(): List<ItemData> {
            // create a Portal
            val portal = Portal(
                portalUri,
                connection = Portal.Connection.Authenticated
            )
            // log an exception and return if the portal loading fails
            portal.load().onFailure {
                Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}", )
                return emptyList()
            }
            val user = portal.user ?: return emptyList()
            // fetch the users content
            val portalUserContent = user.fetchContent().getOrElse { return emptyList() }
            // get the specified folder under the users content
            val folder = portalUserContent.folders.firstOrNull {
                it.title == portalFolder
            } ?: return emptyList()
            // fetch and return content within the specified folder
            return user.fetchContentInFolder(folder.folderId).getOrDefault(emptyList()).filter {
                // filter the content by WebMaps only
                it.type == PortalItemType.WebMap
            }.map {
                ItemData(it.url)
            }
        }
    }
) {

    companion object {

        /**
         * The primary portal url.
         */
        const val portalUri = "https://www.arcgis.com"

        /**
         * Folder under the portal to fetch the portal items from.
         */
        const val portalFolder = "Apollo"
    }

    suspend fun fetchItemData(): List<ItemData> = withContext(dispatcher) {
        return@withContext itemApi.fetchItems()
    }
}
