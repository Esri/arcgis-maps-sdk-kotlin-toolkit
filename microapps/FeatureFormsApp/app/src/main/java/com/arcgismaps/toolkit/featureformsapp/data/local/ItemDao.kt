package com.arcgismaps.toolkit.featureformsapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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
     * Insert or update data in the database. If an item already exists, replace it.
     *
     * @param tasks the ItemData to be inserted or updated.
     */
    @Upsert
    suspend fun upsertAll(tasks: List<ItemData>)
}
