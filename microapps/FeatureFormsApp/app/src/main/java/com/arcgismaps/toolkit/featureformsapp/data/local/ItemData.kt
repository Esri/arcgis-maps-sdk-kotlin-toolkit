package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * the data for the item. Just an URL, but abstracted away
 */
@Entity
data class ItemData(@PrimaryKey val url: String)

/**
 * The API to use to get the items
 */
interface ItemApi {
    suspend fun fetchItems(): List<ItemData>
}
