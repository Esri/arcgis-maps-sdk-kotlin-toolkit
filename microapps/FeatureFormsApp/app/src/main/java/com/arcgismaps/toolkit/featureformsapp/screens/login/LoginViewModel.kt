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

package com.arcgismaps.toolkit.featureformsapp.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.BuildConfig
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val portalSettings: PortalSettings
) : ViewModel() {

    private data class Credentials(val username: String = "", val password: String = "")

    private val authenticatorState = AuthenticatorState()

    private val _loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.NotLoggedIn)
    val loginState = _loginState.asStateFlow()


    private var credentials: Credentials = Credentials()

    init {
        viewModelScope.launch {
            launch {
                authenticatorState.pendingServerTrustChallenge.collect {
                    it?.trust()
                }
            }
            launch {
                authenticatorState.pendingUsernamePasswordChallenge.collect {
                    it?.continueWithCredentials(credentials.username, credentials.password)
                }
            }
        }
    }

    fun loginWithDefaultCredentials() {
        credentials = Credentials(BuildConfig.webMapUser, BuildConfig.webMapPassword)
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            // set a timeout of 20s
            val result = withTimeoutOrNull(20000) {
                authenticatorState.oAuthUserConfiguration = null
                portalSettings.setPortalUrl(portalSettings.defaultPortalUrl)
                portalSettings.setPortalConnection(Portal.Connection.Authenticated)
                val portal =
                    Portal(portalSettings.defaultPortalUrl, Portal.Connection.Authenticated)
                portal.load().onFailure {
                    _loginState.value = LoginState.Failed(it.message ?: "")
                }.onSuccess {
                    _loginState.value = LoginState.Success
                }
            }
            if (result == null) {
                _loginState.value = LoginState.Failed("Operation timed out")
            }
        }
    }

    fun loginWithArcGISEnterprise(url: String, username: String, password: String) {
        credentials = Credentials(username, password)
        viewModelScope.launch(Dispatchers.IO) {
            authenticatorState.oAuthUserConfiguration = null
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
