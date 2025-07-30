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

package com.arcgismaps.toolkit.offlinemapareasapp.di

import android.content.Context
import com.arcgismaps.toolkit.offlinemapareasapp.data.PortalItemRepository
import com.arcgismaps.toolkit.offlinemapareasapp.data.PortalSettings
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.offlinemapareasapp.data.network.ItemRemoteDataSource
import com.arcgismaps.toolkit.offlinemapareasapp.navigation.Navigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the ItemRemoteDataSource
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemRemoteSource

/**
 * Provide an annotation to inject the PortalItemRepository
 */
@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class PortalItemRepo


@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    /**
     * The provider of the ItemRemoteDataSource.
     */
    @Provides
    @ItemRemoteSource
    internal fun provideItemRemoteDataSource(@IoDispatcher dispatcher: CoroutineDispatcher): ItemRemoteDataSource =
        ItemRemoteDataSource(dispatcher)

    /**
     * The provider of the PortalItemRepository.
     */
    @Provides
    @Singleton
    @PortalItemRepo
    internal fun providePortalItemRepository(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @ItemRemoteSource remoteDataSource: ItemRemoteDataSource,
        @ItemCache itemCacheDao: ItemCacheDao,
        @ApplicationContext context: Context
    ): PortalItemRepository =
        PortalItemRepository(dispatcher, remoteDataSource, itemCacheDao, context.filesDir.absolutePath)

    @Singleton
    @Provides
    internal fun providePortalSettings(
        @ApplicationContext context: Context,
    ): PortalSettings = PortalSettings(context = context)

    @Singleton
    @Provides
    internal fun provideNavigator(): Navigator = Navigator()
}
