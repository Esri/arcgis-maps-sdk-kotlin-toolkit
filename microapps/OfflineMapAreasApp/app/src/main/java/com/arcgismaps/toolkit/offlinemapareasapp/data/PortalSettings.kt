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

package com.arcgismaps.toolkit.offlinemapareasapp.data

import android.content.Context
import android.content.SharedPreferences
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.signOut
import com.arcgismaps.toolkit.offlinemapareasapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortalSettings(
    context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("portal_settings", Context.MODE_PRIVATE)

    val defaultPortalUrl: String = context.getString(R.string.agol_portal_url)

    private val urlKey = "url"
    private val connectionKey = "connection"

    fun getPortalConnection() : Portal.Connection {
        val connection = preferences.getInt(connectionKey, 0)
        return if (connection == 0) {
            Portal.Connection.Authenticated
        } else {
            Portal.Connection.Anonymous
        }
    }

    suspend fun setPortalConnection(connection: Portal.Connection) = withContext(Dispatchers.IO) {
        with(preferences.edit()) {
            val value = if (connection is Portal.Connection.Authenticated) {
                0
            } else {
                1
            }
            putInt(connectionKey, value)
            commit()
        }
    }

    fun getPortalUrl(): String {
        return preferences.getString(urlKey, "") ?: ""
    }

    suspend fun setPortalUrl(url: String) = withContext(Dispatchers.IO) {
        with(preferences.edit()) {
            putString(urlKey, url)
            commit()
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        setPortalConnection(Portal.Connection.Authenticated)
        ArcGISEnvironment.authenticationManager.signOut()
    }
}
