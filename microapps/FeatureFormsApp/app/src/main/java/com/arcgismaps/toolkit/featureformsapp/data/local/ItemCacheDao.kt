package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

/**
 * Model to represent a PortalItem Cache entry.
 */
@Entity
data class ItemCacheEntry(
    @PrimaryKey val itemId: String,
    val json: String,
    val thumbnailUri: String,
    val portalUrl: String
)

@Dao
interface ItemCacheDao {

    /**
     * fetch an entry by id.
     *
     * @param itemId the item id.
     * @return the cache entry with the item id.
     */
    @Query("SELECT * FROM itemcacheentry WHERE itemId = :itemId")
    suspend fun getById(itemId: String): ItemCacheEntry?

    /**
     * Insert or update an entry in the database. If an entry already exists, replace it.
     *
     * @param entry the ItemCacheEntry to be inserted or updated.
     */
    @Upsert
    suspend fun upsert(entry: ItemCacheEntry)

    /**
     * Delete all entries.
     */
    @Query("DELETE FROM itemcacheentry")
    suspend fun deleteAll()
}

/**
 * The room database that contains the ItemCacheEntry table.
 */
@Database(entities = [ItemCacheEntry::class], version = 1, exportSchema = false)
abstract class ItemCacheDatabase : RoomDatabase() {
    abstract fun itemCacheDao() : ItemCacheDao
}
