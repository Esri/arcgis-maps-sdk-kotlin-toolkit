/*
 * COPYRIGHT 1995-2024 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureformsapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


/**
 * Provide an annotation to inject the MapViewProxy
 */
@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class MapViewProxy

@Module
@InstallIn(SingletonComponent::class)
class MapViewModule {

    /**
     * The provider of the MapViewProxy
     */
    @Provides
    @Singleton
    @MapViewProxy
    @Suppress("unused")
    internal fun provideMapViewProxy(): com.arcgismaps.toolkit.geocompose.MapViewProxy =
        com.arcgismaps.toolkit.geocompose.MapViewProxy()

}