/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offlinemapareasapp.data.local

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
