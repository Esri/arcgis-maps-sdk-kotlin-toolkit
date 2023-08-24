package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity
data class PortalItemCacheEntry(
    @PrimaryKey val itemId: String,
    val json: String,
    val portalUrl: String
)

@Dao
interface PortalItemDao {

    /**
     * fetch an item by id.
     *
     * @param itemId the item id.
     * @return the cache entry with the item id.
     */
    @Query("SELECT * FROM portalitemcacheentry WHERE itemId = :itemId")
    suspend fun getById(itemId: String): PortalItemCacheEntry?

    /**
     * Insert or update an item in the database. If an entry already exists, replace it.
     *
     * @param entry the PortalItemCacheEntry to be inserted or updated.
     */
    @Upsert
    suspend fun upsert(entry: PortalItemCacheEntry)
}

@Database(entities = [PortalItemCacheEntry::class], version = 1, exportSchema = false)
abstract class PortalItemDatabase : RoomDatabase() {
    abstract fun portalItemDao() : PortalItemDao
}
