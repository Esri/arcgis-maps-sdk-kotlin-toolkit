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
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.BuildConfig
import com.arcgismaps.toolkit.featureformsapp.AppState
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val portalSettings: PortalSettings
) : ViewModel() {

    enum class LoginType {
        AGOL,
        ENTERPRISE,
        NONE
    }

    val authenticatorState = AuthenticatorState()

    private val _loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.NotLoggedIn)
    val loginState = _loginState.asStateFlow()

    private var loginType = LoginType.NONE

    private var username : String = ""
    private var password : String = ""

    init {
        viewModelScope.launch {
            launch {
                authenticatorState.pendingServerTrustChallenge.collect {
                    Log.e("TAG", "server trust $it: ")
                    it?.trust()
                }
            }
            launch {
                authenticatorState.pendingClientCertificateChallenge.collect {
                    Log.e("TAG", "client cert $it: ")
                }
            }
            authenticatorState.pendingUsernamePasswordChallenge.collect {
                Log.e("TAG", "username chal: ${it?.url}")
                //if (loginType == LoginType.AGOL) {
                    //it?.continueWithCredentials(BuildConfig.webMapUser, BuildConfig.webMapPassword)
                    it?.continueWithCredentials(username, password)
                //}
            }
        }
    }

    fun loginWithDefaultCredentials() {
        loginType = LoginType.AGOL
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            // set a timeout of 20s
            val result = withTimeoutOrNull(20000) {
                //delay(20000)
                authenticatorState.oAuthUserConfiguration = null
                portalSettings.setPortalUrl(portalSettings.defaultPortalUrl)
                val portal = Portal(portalSettings.defaultPortalUrl, Portal.Connection.Authenticated)
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

    fun loginWithArcGISEnterprise(url: String, username : String, password : String) {
        this.username = username
        this.password = password
        loginType = LoginType.ENTERPRISE
        viewModelScope.launch(Dispatchers.IO) {
            authenticatorState.oAuthUserConfiguration = null
            portalSettings.setPortalUrl(url)
            val portal = Portal(url, Portal.Connection.Authenticated)
            portal.load().onFailure {
                _loginState.value = LoginState.Failed(it.message ?: "")
            }.onSuccess {
                _loginState.value = LoginState.Success
            }
            launch {
                portal.loadStatus.collect {
                    Log.e("TAG", "loginWithArcGISEnterprise: $it", )
                }
            }
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Failed(val message: String) : LoginState()
    object NotLoggedIn : LoginState()
}

