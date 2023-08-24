package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
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

@Dao
interface ItemDao {

    /**
     * Observes list of ItemData.
     *
     * @return all ItemData.
     */
    @Query("SELECT * FROM itemdata")
    fun observeAll(): Flow<List<ItemData>>

    /**
     * Select all items from the tasks table.
     *
     * @return all items.
     */
    @Query("SELECT * FROM itemdata")
    suspend fun getAll(): List<ItemData>

    /**
     * Insert or update data in the database. If an item already exists, replace it.
     *
     * @param tasks the ItemData to be inserted or updated.
     */
    @Upsert
    suspend fun upsertAll(tasks: List<ItemData>)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM itemdata")
    suspend fun deleteAll()
}

@Database(entities = [ItemData::class], version = 1, exportSchema = false)
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
