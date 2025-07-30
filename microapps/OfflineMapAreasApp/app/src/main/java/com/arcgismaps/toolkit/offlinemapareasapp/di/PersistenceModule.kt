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

package com.arcgismaps.toolkit.offlinemapareasapp.di

import android.content.Context
import androidx.room.Room
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.ItemCacheDatabase
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.UrlHistoryDao
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.UrlHistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the ItemCache Data Access Object.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemCache

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class UrlHistory

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    /**
     * The provider of the item cache dao.
     */
    @Singleton
    @Provides
    @ItemCache
    fun provideItemCacheDao(database: ItemCacheDatabase): ItemCacheDao = database.itemCacheDao()

    /**
     * The provider of the ItemCacheDatabase.
     */
    @Singleton
    @Provides
    fun provideItemCacheDataBase(@ApplicationContext context: Context): ItemCacheDatabase {
        return Room.databaseBuilder(
            context,
            ItemCacheDatabase::class.java,
            "portal_items.db"
        ).build()
    }

    @Singleton
    @Provides
    @UrlHistory
    fun provideUrlHistoryDao(database: UrlHistoryDatabase) : UrlHistoryDao = database.urlHistoryDao()

    @Singleton
    @Provides
    fun provideUrlHistoryDatabase(@ApplicationContext context: Context): UrlHistoryDatabase {
        return Room.databaseBuilder(
            context,
            UrlHistoryDatabase::class.java,
            "url_history.db"
        ).build()
    }
}
