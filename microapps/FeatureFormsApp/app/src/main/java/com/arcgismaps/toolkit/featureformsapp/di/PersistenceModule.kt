package com.arcgismaps.toolkit.featureformsapp.di

import android.content.Context
import androidx.room.Room
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDatabase
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
            context.applicationContext,
            ItemCacheDatabase::class.java,
            "portal_items.db"
        ).build()
    }
}
