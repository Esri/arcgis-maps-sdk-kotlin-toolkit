/*
 *
 *  Copyright 2026 Esri
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
/*
 * COPYRIGHT 1995-2026 ESRI
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

package com.esri.microappslib

import android.content.Context
import androidx.startup.Initializer
import com.arcgismaps.sharedtestutilities.OpenBaoTestSecretsStore

/**
 * Initializes the OpenBao access token. This initializer is automatically invoked by
 * [AndroidX Startup](https://developer.android.com/topic/libraries/app-startup) when any app that
 * depends on this library is launched.
 *
 * @since 300.2.0
 */
internal class OpenBaoInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        OpenBaoTestSecretsStore.accessToken = BuildConfig.vaultToken
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
