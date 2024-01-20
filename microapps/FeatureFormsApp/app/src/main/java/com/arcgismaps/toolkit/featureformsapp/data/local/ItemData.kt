package com.arcgismaps.toolkit.featureformsapp.data.local

import com.arcgismaps.portal.Portal

/**
 * the data for the item. Just an URL, but abstracted away
 */
data class ItemData(val url: String)

/**
 * The API to use to get the items
 */
interface ItemApi {
    suspend fun fetchItems(portalUri: String, connection : Portal.Connection): List<ItemData>
}
