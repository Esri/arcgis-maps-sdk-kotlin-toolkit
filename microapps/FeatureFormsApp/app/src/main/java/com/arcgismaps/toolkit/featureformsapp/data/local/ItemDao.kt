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

    @Insert
    suspend fun insert(items: ItemData) : Long

    /**
     * Observes list of ItemData.
     *
     * @return all ItemData.
     */
    @Query("SELECT * FROM itemdata")
    fun observeAll(): Flow<List<ItemData>>


    @Query("SELECT COUNT(*) FROM itemdata")
    suspend fun getCount() : Int

    /**
     * Delete all items.
     */
    @Query("DELETE FROM itemdata")
    suspend fun deleteAll()


    @Transaction
    suspend fun deleteAndInsert(items: List<ItemData>) : List<Long> {
        deleteAll()
        return items.map {
            insert(it)
        }
    }
}

@Database(entities = [ItemData::class], version = 1, exportSchema = false)
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
