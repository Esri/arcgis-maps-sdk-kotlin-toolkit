package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity
data class ItemCacheEntry(
    @PrimaryKey val itemId: String,
    val json: String,
    val portalUrl: String
)

@Dao
interface ItemCacheDao {

    /**
     * fetch an item by id.
     *
     * @param itemId the item id.
     * @return the cache entry with the item id.
     */
    @Query("SELECT * FROM itemcacheentry WHERE itemId = :itemId")
    suspend fun getById(itemId: String): ItemCacheEntry?

    /**
     * Insert or update an item in the database. If an entry already exists, replace it.
     *
     * @param entry the ItemCacheEntry to be inserted or updated.
     */
    @Upsert
    suspend fun upsert(entry: ItemCacheEntry)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM itemcacheentry")
    suspend fun deleteAll()
}

@Database(entities = [ItemCacheEntry::class], version = 1, exportSchema = false)
abstract class ItemCacheDatabase : RoomDatabase() {
    abstract fun itemCacheDao() : ItemCacheDao
}
