package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Model to represent a PortalItem Cache entry.
 */
@Entity
data class ItemCacheEntry(
    @PrimaryKey val itemId: String,
    val json: String,
    val portalUrl: String
)

@Dao
interface ItemCacheDao {

    /**
     * Insert an item into the itemcacheentry table.
     *
     * @param item the ItemCacheEntry type to insert.
     */
    @Insert
    suspend fun insert(item: ItemCacheEntry) : Long

    /**
     * Observes list of ItemData.
     *
     * @return all ItemData.
     */
    @Query("SELECT * FROM itemcacheentry")
    fun observeAll(): Flow<List<ItemCacheEntry>>

    /**
     * fetch an entry by id.
     *
     * @param itemId the item id.
     * @return the cache entry with the item id.
     */
    @Query("SELECT * FROM itemcacheentry WHERE itemId = :itemId")
    suspend fun getById(itemId: String): ItemCacheEntry?

    /**
     * Get the number of items in the table.
     *
     * @return the number of items.
     */
    @Query("SELECT COUNT(*) FROM itemcacheentry")
    suspend fun getCount() : Int


    /**
     * Deletes all existing items in the table and inserts the new list [items].
     *
     * @param items the list of items to insert.
     * @return the list of row id's that were inserted.
     */
    @Transaction
    suspend fun deleteAndInsert(items: List<ItemCacheEntry>) : List<Long> {
        deleteAll()
        return items.map {
            insert(it)
        }
    }

    /**
     * Delete all entries.
     */
    @Query("DELETE FROM itemcacheentry")
    suspend fun deleteAll()
}

/**
 * The room database that contains the ItemCacheEntry table.
 */
@Database(entities = [ItemCacheEntry::class], version = 2, exportSchema = false)
abstract class ItemCacheDatabase : RoomDatabase() {
    abstract fun itemCacheDao() : ItemCacheDao
}
