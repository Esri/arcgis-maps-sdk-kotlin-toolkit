package com.arcgismaps.toolkit.featureformsapp.di

import android.content.Context
import androidx.room.Room
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemCacheDatabase
import com.arcgismaps.toolkit.featureformsapp.data.local.UrlHistoryDao
import com.arcgismaps.toolkit.featureformsapp.data.local.UrlHistoryDatabase
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