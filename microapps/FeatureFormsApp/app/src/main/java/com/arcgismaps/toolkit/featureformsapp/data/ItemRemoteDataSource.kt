package com.arcgismaps.toolkit.featureformsapp.data

import android.util.Log
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItemType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ItemRemoteDataSource(
    private val dispatcher: CoroutineDispatcher, private val itemApi: ItemApi = object : ItemApi {
        override suspend fun fetchItems(): List<ItemData> {
            val portal = Portal(
                "https://runtimecoretest.maps.arcgis.com",
                connection = Portal.Connection.Authenticated
            )
            portal.load().onFailure { return emptyList() }
            val user = portal.user ?: return emptyList()
            val portalUserContent = user.fetchContent().getOrElse { return emptyList() }
            val folder = portalUserContent.folders.first {
                it.title == "Apollo"
            }
            Log.e("TAG", "testGetPortal: folder id ${folder.folderId}")
            return user.fetchContentInFolder(folder.folderId).getOrDefault(emptyList()).filter {
                it.type == PortalItemType.WebMap
            }.map {
                ItemData(it.url, DataSourceType.Remote)
            }
        }
    }
) {

    suspend fun fetchItemData(): List<ItemData> = withContext(dispatcher) {
        itemApi.fetchItems()
    }
}
