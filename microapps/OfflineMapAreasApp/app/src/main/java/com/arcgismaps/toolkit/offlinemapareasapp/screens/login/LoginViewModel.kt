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

package com.arcgismaps.toolkit.offlinemapareasapp.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.offlinemapareasapp.data.PortalSettings
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.UrlEntry
import com.arcgismaps.toolkit.offlinemapareasapp.data.local.UrlHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val portalSettings: PortalSettings,
    private val urlHistoryDao: UrlHistoryDao
) : ViewModel() {
    val authenticatorState = AuthenticatorState()

    val urlHistory: StateFlow<List<String>> = urlHistoryDao.observeAll().map { urlEntries ->
        urlEntries.map {
            it.url
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = emptyList()
    )

    private val _loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.NotLoggedIn)
    val loginState = _loginState.asStateFlow()

    private val oAuthRedirectUri = "featureformsapp://auth"
    private val clientId = "iFmvhJGQEKGK1Ahf"

    /**
     * Save this url to the search history.
     */
    fun addUrlToHistory(url: String) {
        viewModelScope.launch {
            if (url.isNotEmpty()) {
                urlHistoryDao.insert(UrlEntry(url))
            }
        }
    }

    /**
     * Authenticate the user with the given portal [url]. Default [url] is ArcGIS Online.
     */
    fun login(url: String = portalSettings.defaultPortalUrl, useOAuth: Boolean) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            authenticatorState.oAuthUserConfiguration =
                if (useOAuth)
                    OAuthUserConfiguration(
                        portalUrl = url,
                        clientId = clientId,
                        redirectUrl = oAuthRedirectUri,
                    )
                else null
            portalSettings.setPortalUrl(url)
            portalSettings.setPortalConnection(Portal.Connection.Authenticated)
            val portal = Portal(url, Portal.Connection.Authenticated)
            portal.load().onFailure {
                _loginState.value = LoginState.Failed(it.message ?: "")
            }.onSuccess {
                _loginState.value = LoginState.Success
            }
        }
    }

    /**
     * Skip authentication and use the portal as an anonymous user to load any public content.
     */
    fun skipSignIn() {
        viewModelScope.launch {
            portalSettings.setPortalUrl(portalSettings.defaultPortalUrl)
            portalSettings.setPortalConnection(Portal.Connection.Anonymous)
            _loginState.value = LoginState.Success
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Failed(val message: String) : LoginState()
    object NotLoggedIn : LoginState()
}
