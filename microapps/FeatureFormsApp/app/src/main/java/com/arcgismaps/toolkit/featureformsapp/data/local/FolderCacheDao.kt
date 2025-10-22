/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.data.local

import java.time.Instant
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.arcgismaps.portal.PortalFolder
import kotlinx.coroutines.flow.Flow

/**
 * Model to represent a PortalItem Cache entry.
 */
@Entity
data class FolderCacheEntry(
    @PrimaryKey val folderId: String,
    val username: String,
    val title: String,
    val creationDate: Instant? = null
) {
    fun toPortalFolder(): PortalFolder {
        return PortalFolder(
            folderId = folderId,
            username = username,
            title = title,
            creationDate = creationDate
        )
    }
}

class DateConverter {

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }


    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()
}

@Dao
interface FolderCacheDao {

    /**
     * Insert an item into the table.
     *
     * @param item the FolderCacheEntry type to insert.
     */
    @Insert
    suspend fun insert(item: FolderCacheEntry): Long

    /**
     * Observes list of folder items.
     *
     * @return all items.
     */
    @Query("SELECT * FROM foldercacheentry")
    fun observeAll(): Flow<List<FolderCacheEntry>>

    /**
     * fetch an entry by id.
     *
     * @param folderId the folder id.
     * @return the folder entry with the folder id.
     */
    @Query("SELECT * FROM foldercacheentry WHERE folderId = :folderId")
    suspend fun getById(folderId: String): FolderCacheEntry?

    /**
     * Get the number of items in the table.
     *
     * @return the number of items.
     */
    @Query("SELECT COUNT(*) FROM foldercacheentry")
    suspend fun getCount(): Int


    /**
     * Deletes all existing items in the table and inserts the new list [items].
     *
     * @param items the list of items to insert.
     * @return the list of row id's that were inserted.
     */
    @Transaction
    suspend fun deleteAndInsert(items: List<FolderCacheEntry>): List<Long> {
        deleteAll()
        return items.map {
            insert(it)
        }
    }

    /**
     * Delete all entries.
     */
    @Query("DELETE FROM foldercacheentry")
    suspend fun deleteAll()
}

/**
 * The room database that contains the [FolderCacheEntry] table.
 */
@Database(entities = [FolderCacheEntry::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class FolderCacheDatabase : RoomDatabase() {
    abstract fun folderCacheDao(): FolderCacheDao
}
