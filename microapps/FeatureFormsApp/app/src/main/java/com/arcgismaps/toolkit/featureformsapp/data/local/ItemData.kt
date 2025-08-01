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

package com.arcgismaps.toolkit.featureformsapp.data.local

import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalFolder
import java.time.Instant

//sealed class ItemData(val id: String)
//
//class PortalItemData(id: String) : ItemData(id)
//
//class FolderData(
//    id: String,
//    val username: String,
//    val title: String,
//    val creationDate: Instant? = null
//) : ItemData(id)

class UserContent(
    val items : List<PortalItem>,
    val folders: List<PortalFolder>
)

/**
 * The API to use to get the items
 */
interface ItemApi {

    suspend fun fetchContent(
        portalUri: String,
        connection: Portal.Connection
    ): UserContent

    suspend fun fetchItemsInFolder(
        portalUri: String,
        folderId: String,
    ): List<PortalItem>
}
