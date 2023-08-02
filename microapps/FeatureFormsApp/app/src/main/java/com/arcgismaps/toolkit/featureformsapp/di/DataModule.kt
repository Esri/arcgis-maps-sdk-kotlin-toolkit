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

import com.arcgismaps.toolkit.featureformsapp.data.ItemDataSource
import com.arcgismaps.toolkit.featureformsapp.data.ItemRepository
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the PortalItem data source
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PortalItemDataSource

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
class DataModule() {
    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Provides
    @PortalItemDataSource
    internal fun provideItemDataSource(@IoDispatcher dispatcher: CoroutineDispatcher): ItemDataSource =
        ItemDataSource(dispatcher)
    
    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Provides
    @PortalItemRepository
    internal fun provideItemRepository(
        @ApplicationScope applicationScope: CoroutineScope,
        @PortalItemDataSource itemDataSource: ItemDataSource
    ): ItemRepository =
        ItemRepository(applicationScope, itemDataSource)
    
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
