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

import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemRemoteSource

/**
 * Provide an annotation to inject the PortalItem repository
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemRepo

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PortalItemRepo

/**
 * The singleton portal item use case provider
 */
@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @ItemRemoteSource
    internal fun provideItemRemoteDataSource(@IoDispatcher dispatcher: CoroutineDispatcher): ItemRemoteDataSource =
        ItemRemoteDataSource(dispatcher)
    
    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Provides
    @ItemRepo
    internal fun provideItemRepository(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @ItemLocalSource itemLocalSource: ItemDao,
        @ItemRemoteSource remoteDataSource: ItemRemoteDataSource
    ): ItemRepository =
        ItemRepository(dispatcher, itemLocalSource, remoteDataSource)

    @Provides
    @PortalItemRepo
    internal fun providePortalItemRepository(
        @ApplicationScope coroutineScope: CoroutineScope,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @ItemRepo itemRepository: ItemRepository,
        @ItemCache itemCacheDao: ItemCacheDao,
    ): PortalItemRepository =
        PortalItemRepository(coroutineScope, dispatcher, itemRepository, itemCacheDao)
    
    /**
     * The provider of the PortalItem use case, scoped to the navigation graph lifetime by means of the
     * `@Singleton` annotation.
     */
    @Singleton
    @Provides
    fun providePortalItemUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope,
        @PortalItemRepo portalItemRepository: PortalItemRepository
    ): PortalItemUseCase = PortalItemUseCase(
        dispatcher,
        applicationScope,
        portalItemRepository
    )
}
