package com.arcgismaps.toolkit.featureformsapp.data.network

import android.util.Log
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemApi
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ItemRemoteDataSource(
    private val dispatcher: CoroutineDispatcher, private val itemApi: ItemApi = object : ItemApi {
        override suspend fun fetchItems(): List<ItemData> {
            val portal = Portal(
                portalUri,
                connection = Portal.Connection.Authenticated
            )
            portal.load().onFailure {
                Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}", )
                return emptyList()
            }
            val user = portal.user ?: return emptyList()
            val portalUserContent = user.fetchContent().getOrElse { return emptyList() }
            val folder = portalUserContent.folders.firstOrNull {
                it.title == portalFolder
            } ?: return emptyList()
            return user.fetchContentInFolder(folder.folderId).getOrDefault(emptyList()).filter {
                it.type == PortalItemType.WebMap
            }.map {
                ItemData(it.url)
            }
        }
    }
) {

    companion object {
        const val portalUri = "https://www.arcgis.com"
        const val portalFolder = "Apollo"
    }

    suspend fun fetchItemData(): List<ItemData> = withContext(dispatcher) {
        return@withContext itemApi.fetchItems()
    }
}
