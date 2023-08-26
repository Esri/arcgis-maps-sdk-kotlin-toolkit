package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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

/**
 * Data Access Object for the itemdata table.
 */
@Dao
interface ItemDao {

    /**
     * Insert an item into the itemdata table.
     *
     * @param item the ItemData type to insert.
     */
    @Insert
    suspend fun insert(item: ItemData) : Long

    /**
     * Observes list of ItemData.
     *
     * @return all ItemData.
     */
    @Query("SELECT * FROM itemdata")
    fun observeAll(): Flow<List<ItemData>>

    /**
     * Get the number of items in the table.
     *
     * @return the number of items.
     */
    @Query("SELECT COUNT(*) FROM itemdata")
    suspend fun getCount() : Int

    /**
     * Delete all items.
     */
    @Query("DELETE FROM itemdata")
    suspend fun deleteAll()

    /**
     * Deletes all existing items in the table and inserts the new list [items].
     *
     * @param items the list of items to insert.
     * @return the list of row id's that were inserted.
     */
    @Transaction
    suspend fun deleteAndInsert(items: List<ItemData>) : List<Long> {
        deleteAll()
        return items.map {
            insert(it)
        }
    }
}

/**
 * The Room Database that contains the ItemData table.
 *
 */
@Database(entities = [ItemData::class], version = 1, exportSchema = false)
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
