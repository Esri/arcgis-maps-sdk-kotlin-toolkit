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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.BuildConfig
import com.arcgismaps.toolkit.featureformsapp.LoginState
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val portalSettings: PortalSettings
) : ViewModel() {

    private val authenticatorState = AuthenticatorState()

    private val _loginState : MutableStateFlow<LoginState> = MutableStateFlow(LoginState.NotLoggedIn)
    val loginState = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            authenticatorState.pendingUsernamePasswordChallenge.collect {
                it?.continueWithCredentials(BuildConfig.webMapUser, BuildConfig.webMapPassword)
            }
        }
    }

    fun loginWithDefaultCredentials() {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            authenticatorState.oAuthUserConfiguration = null
            portalSettings.setPortalUrl(portalSettings.defaultPortalUrl)
            val portal = Portal(ItemRemoteDataSource.portalUri, Portal.Connection.Authenticated)
            portal.load().onFailure {
                _loginState.value = LoginState.NotLoggedIn
            }.onSuccess {
                _loginState.value = LoginState.LoggedIn
            }
        }
    }
}
