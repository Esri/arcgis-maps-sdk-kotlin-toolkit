/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.data

import android.content.Context
import android.content.SharedPreferences
import com.arcgismaps.toolkit.featureformsapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortalSettings(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("portal_settings", Context.MODE_PRIVATE)

    val defaultPortalUrl: String = context.getString(R.string.agol_portal_url)

    private val urlKey = "url"

    fun getPortalUrl(): String {
        return preferences.getString(urlKey, "") ?: ""
    }

    suspend fun setPortalUrl(url: String) = withContext(Dispatchers.IO) {
        with(preferences.edit()) {
            putString(urlKey, url)
            commit()
        }
    }
}
