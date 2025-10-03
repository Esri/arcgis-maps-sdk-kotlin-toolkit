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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalUser
import com.arcgismaps.toolkit.authentication.signOut
import com.arcgismaps.toolkit.featureformsapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.portalPreferences: DataStore<Preferences> by preferencesDataStore(name = "portal_settings")
private val URL_KEY = stringPreferencesKey("url")
private val PORTAL_USER_KEY = stringPreferencesKey("portal_user")
private val PORTAL_CONNECTION_KEY = intPreferencesKey("portal_connection")

class PortalSettings(
    context: Context,
    scope: CoroutineScope
) {
    private val preferences = context.portalPreferences

    private val _user: MutableStateFlow<PortalUser?> = MutableStateFlow(null)

    /**
     * The current portal user, or null if not logged in. Observe this to get updates when the user changes.
     *
     * See [getPortalUser] for how to retrieve the user directly.
     */
    val user: StateFlow<PortalUser?> = _user.asStateFlow()

    val defaultPortalUrl: String = context.getString(R.string.agol_portal_url)

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            // Load the initial portal user from preferences, if available
            _user.value = getPortalUser()
        }
    }

    /**
     * Retrieves the current portal connection state.
     */
    suspend fun getPortalConnection(): Portal.Connection? = withContext(Dispatchers.IO) {
        val connection = preferences.data.first()[PORTAL_CONNECTION_KEY] ?: return@withContext null
        return@withContext if (connection == 0) {
            Portal.Connection.Authenticated
        } else {
            Portal.Connection.Anonymous
        }
    }

    /**
     * Sets the portal connection state.
     */
    suspend fun setPortalConnection(connection: Portal.Connection) =
        withContext(Dispatchers.IO) {
            preferences.edit { settings ->
                settings[PORTAL_CONNECTION_KEY] =
                    if (connection is Portal.Connection.Authenticated) {
                        0
                    } else {
                        1
                    }
            }
        }

    /**
     * Retrieves the current portal user, or null if not logged in. See [user] for a flow that emits
     * updates to the user state.
     */
    suspend fun getPortalUser(): PortalUser? = withContext(Dispatchers.IO) {
        preferences.data.firstOrNull()?.let { settings ->
            val portalUrl = settings[URL_KEY] ?: return@withContext null
            val connection = settings[PORTAL_CONNECTION_KEY] ?: return@withContext null
            val userJson = settings[PORTAL_USER_KEY] ?: return@withContext null
            val portal = Portal(
                portalUrl,
                if (connection == 0) {
                    Portal.Connection.Authenticated
                } else {
                    Portal.Connection.Anonymous
                }
            )
            PortalUser.fromJsonOrNull(userJson, portal)
        }
    }

    /**
     * Sets the current portal user and updates the preferences.
     */
    suspend fun setPortalUser(user: PortalUser) = withContext(Dispatchers.IO) {
        preferences.edit { settings ->
            settings[PORTAL_USER_KEY] = user.toJson()
            settings[URL_KEY] = user.portal.url
            settings[PORTAL_CONNECTION_KEY] =
                if (user.portal.connection is Portal.Connection.Authenticated) {
                    0 // Authenticated
                } else {
                    1 // Anonymous
                }
        }
        _user.value = user
    }

    /**
     * Signs out the current user and clears the stored portal settings.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        preferences.edit { settings ->
            settings.remove(URL_KEY)
            settings.remove(PORTAL_USER_KEY)
            settings.remove(PORTAL_CONNECTION_KEY)
        }
        _user.value = null
        ArcGISEnvironment.authenticationManager.signOut()
    }
}
