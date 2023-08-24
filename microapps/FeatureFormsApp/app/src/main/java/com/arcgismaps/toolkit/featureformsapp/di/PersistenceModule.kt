package com.arcgismaps.toolkit.featureformsapp.di

import android.content.Context
import androidx.room.Room
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.ItemDatabase
import com.arcgismaps.toolkit.featureformsapp.data.local.PortalItemDao
import com.arcgismaps.toolkit.featureformsapp.data.local.PortalItemDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provide an annotation to inject the PortalItem data source
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemLocalSource

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PortalItemLocalSource

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    /**
     * The provider of the PortalItem data source. Only used below.
     */
    @Singleton
    @Provides
    @ItemLocalSource
    fun provideItemDao(database: ItemDatabase): ItemDao = database.itemDao()

    @Singleton
    @Provides
    @PortalItemLocalSource
    fun providePortalItemDao(database: PortalItemDatabase): PortalItemDao = database.portalItemDao()

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): ItemDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ItemDatabase::class.java,
            "items.db"
        ).build()
    }

    @Singleton
    @Provides
    fun providePortalItemDataBase(@ApplicationContext context: Context): PortalItemDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PortalItemDatabase::class.java,
            "portal_items.db"
        ).build()
    }
}
