/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.offlinemapareasapp.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Model to represent a url entry.
 */
@Entity
data class UrlEntry(
    @PrimaryKey val url: String,
)

@Dao
interface UrlHistoryDao {

    /**
     * Insert an item into the url entry table.
     *
     * @param entry the url to insert.
     * @return the rowId for the inserted item.
     */
    @Upsert
    suspend fun insert(entry: UrlEntry) : Long

    /**
     * Observes list of url entries.
     *
     * @return all entries.
     */
    @Query("SELECT * FROM urlentry")
    fun observeAll(): Flow<List<UrlEntry>>

    /**
     * Deletes an entry.
     */
    @Delete
    suspend fun delete(entry: UrlEntry)
}

/**
 * The room database that contains the [UrlEntry] table.
 */
@Database(entities = [UrlEntry::class], version = 1, exportSchema = false)
abstract class UrlHistoryDatabase : RoomDatabase() {
    abstract fun urlHistoryDao() : UrlHistoryDao
}
