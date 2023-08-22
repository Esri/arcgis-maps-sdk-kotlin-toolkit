package com.arcgismaps.toolkit.featureformsapp.data

enum class DataSourceType {
    Local,
    Remote
}

/**
 * the data for the item. Just an URL, but abstracted away
 */
data class ItemData(val url: String, val type : DataSourceType)

/**
 * The API to use to get the items
 */
interface ItemApi {
    suspend fun fetchItems(): List<ItemData>
}
