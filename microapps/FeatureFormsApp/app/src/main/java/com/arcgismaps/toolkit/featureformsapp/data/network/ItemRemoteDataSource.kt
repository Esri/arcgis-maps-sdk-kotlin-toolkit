/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureformsapp.data.network

import android.util.Log
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItemType
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemApi
import com.arcgismaps.toolkit.featureformsapp.data.local.UserContent
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Main data source for accessing the portal item data from the network.
 */
class ItemRemoteDataSource(
    private val dispatcher: CoroutineDispatcher
) : ItemApi
//    private val itemApi: ItemApi = object : ItemApi {
//        override suspend fun fetchItems(
//            portalUri: String,
//            connection: Portal.Connection
//        ): List<ItemData> {
//            // create a Portal
//            val portal = Portal(
//                portalUri,
//                connection = connection
//            )
//            // log an exception and return if the portal loading fails
//            portal.load().onFailure {
//                Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}")
//                return emptyList()
//            }
////            portal.findItems(
////                PortalQueryParameters(
////                    query = "type:\"Web Map\""
////                )
//////
//////                PortalQueryParameters(
//////                    query = "owner:${portal.user!!.username} -type:\"Web Map\""
//////                ).apply {
//////                    this.setQueryForItemsOfTypes()
//////                }
////            ).onSuccess { result ->
////                Log.e("TAG", "success query: ${result.totalResults}", )
////                result.results.forEach { res ->
////                    Log.e("TAG", "query: ${res.title}, ${res.type}")
////                }
////            }.onFailure { error ->
////                Log.e("TAG", "FAILED query:", error)
////            }
//            val user = portal.user ?: return emptyList()
//            // fetch the users content
//            val portalUserContent = user.fetchContent().getOrElse { return emptyList() }
//            // get the specified folder under the users content
////            val folder = portalUserContent.folders.firstOrNull {
////                it.title == portalFolder
////            }
//            //Log.e("TAG", "fetchItems: ${folder?.title}")
////            return if (folder != null) {
////                // fetch and return content within the specified folder
////                user.fetchContentInFolder(folder.folderId).getOrDefault(emptyList()).filter {
////                    // filter the content by WebMaps only
////                    it.type == PortalItemType.WebMap
////                }.map {
////                    ItemData(it.url, folder = folder.title)
////                }
////            } else {
////                val folderItems = portalUserContent.folders.flatMap {
////                    user.fetchContentInFolder(it.folderId).getOrNull()?.map {
////                        ItemData(it.url, folder = it.title)
////                    } ?: emptyList()
////                }
////                var items = portalUserContent.items.filter {
////                    it.type == PortalItemType.WebMap
////                }.map {
////                    ItemData(it.url, folder = portalUserContent.currentFolder?.title ?: "")
////                }
////                items + folderItems
////            }
//            val folders = portalUserContent.folders.map { folder ->
//                FolderData(
//                    folder.folderId,
//                    folder.username,
//                    folder.title,
//                    folder.creationDate
//                )
//            }
//            val items = portalUserContent.items.filter {
//                it.type == PortalItemType.WebMap
//            }.map {
//                PortalItemData(it.url)
//            }
//            return folders + items
//        }
//
//        override suspend fun fetchItemsInFolder(
//            portalUri: String,
//            folderId: String
//        ): List<PortalItemData> {
//            // create a Portal
//            val portal = Portal(
//                portalUri,
//                connection = Portal.Connection.Authenticated
//            )
//            // log an exception and return if the portal loading fails
//            portal.load().onFailure {
//                Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}")
//                return emptyList()
//            }
//            val user = portal.user ?: return emptyList()
//            // fetch the users content
//            val items = user.fetchContentInFolder(folderId).getOrDefault(emptyList())
//            return items.map {
//                PortalItemData(it.url)
//            }
//        }
//    }
{
    //    suspend fun fetchItemData(portalUri: String, connection: Portal.Connection): List<ItemData> =
//        withContext(dispatcher) {
//            itemApi.fetchItems(portalUri, connection)
//        }
//
//    suspend fun fetchItemsInFolder(
//        portalUri: String,
//        folderId: String
//    ): List<PortalItemData> = withContext(dispatcher) {
//        itemApi.fetchItemsInFolder(portalUri, folderId)
//    }
    override suspend fun fetchContent(
        portalUri: String,
        connection: Portal.Connection
    ): UserContent {
        //create a Portal
        val portal = Portal(
            portalUri,
            connection = connection
        )
        // log an exception and return if the portal loading fails
        portal.load().onFailure {
            Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}")
            return emptyUserContent()
        }
        val user = portal.user ?: return emptyUserContent()
        // fetch the users content
        val portalUserContent = user.fetchContent().getOrElse { return emptyUserContent() }
        val items = portalUserContent.items.filter {
            it.type == PortalItemType.WebMap
        }
        return UserContent(
            items = items,
            folders = portalUserContent.folders
        )
    }

    override suspend fun fetchItemsInFolder(
        portalUri: String,
        folderId: String
    ): List<PortalItem> {
        // create a Portal
        val portal = Portal(
            portalUri,
            connection = Portal.Connection.Authenticated
        )
        // log an exception and return if the portal loading fails
        portal.load().onFailure {
            Log.e("ItemRemoteDataSource", "error in fetchItems: ${it.message}")
            return emptyList()
        }
        val user = portal.user ?: return emptyList()
        // fetch the users content
        val items = user.fetchContentInFolder(folderId).getOrDefault(emptyList()).filter {
            it.type == PortalItemType.WebMap
        }
        items.forEach {
            Log.e("TAG", "fetchItemsInFolder: ${it.folderId}", )
        }
        return items
    }

    fun emptyUserContent(): UserContent {
        return UserContent(
            items = emptyList(),
            folders = emptyList()
        )
    }
}
