package com.arcgismaps.toolkit.featureformsapp.di

import android.content.Context
import androidx.room.Room
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDatabase
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the Local Item Data Access Object.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemLocalSource

/**
 * Provide an annotation to inject the ItemCache Data Access Object.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemCache

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    /**
     * The provider of the item dao.
     */
    @Singleton
    @Provides
    @ItemLocalSource
    fun provideItemDao(database: ItemDatabase): ItemDao = database.itemDao()

    /**
     * The provider of the item cache dao.
     */
    @Singleton
    @Provides
    @ItemCache
    fun provideItemCacheDao(database: ItemCacheDatabase): ItemCacheDao = database.itemCacheDao()

    /**
     * The provider of the ItemDatabase.
     */
    @Singleton
    @Provides
    fun provideItemDataBase(@ApplicationContext context: Context): ItemDatabase {
        return Room.databaseBuilder(
            context,
            ItemDatabase::class.java,
            "items.db"
        ).build()
    }

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
}
