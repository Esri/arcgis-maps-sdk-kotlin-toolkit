/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureformsapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemData
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDatabase
import com.arcgismaps.toolkit.featureformsapp.data.local.getListOfMaps
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the PortalItem data source
 */
@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class PortalItemDataSource

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PortalItemRemoteDataSource

/**
 * Provide an annotation to inject the PortalItem repository
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PortalItemRepository

/**
 * The singleton portal item use case provider
 */
@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @PortalItemRemoteDataSource
    internal fun provideItemRemoteDataSource(@IoDispatcher dispatcher: CoroutineDispatcher): ItemRemoteDataSource =
        ItemRemoteDataSource(dispatcher)
    
    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Provides
    @PortalItemRepository
    internal fun provideItemRepository(
        @ApplicationScope applicationScope: CoroutineScope,
        @PortalItemDataSource itemDataSource: ItemDao,
        @PortalItemRemoteDataSource remoteDataSource: ItemRemoteDataSource
    ): ItemRepository =
        ItemRepository(applicationScope, itemDataSource, remoteDataSource)
    
    /**
     * The provider of the PortalItem use case, scoped to the navigation graph lifetime by means of the
     * `@Singleton` annotation.
     */
    @Singleton
    @Provides
    fun providePortalItemUseCase(
        @ApplicationScope applicationScope: CoroutineScope,
        @PortalItemRepository itemRepository: ItemRepository
    ): PortalItemUseCase = PortalItemUseCase(
        applicationScope,
        itemRepository
    )
}

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Singleton
    @Provides
    @PortalItemDataSource
    fun provideItemDao(database: ItemDatabase): ItemDao = database.itemDao()

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context, itemDaoProvider : Lazy<ItemDao>): ItemDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ItemDatabase::class.java,
            "items.db"
        ).addCallback(object : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    itemDaoProvider.get().upsertAll(getListOfMaps().map {
                        ItemData(it)
                    })
                }
            }
        }).build()
    }
}
