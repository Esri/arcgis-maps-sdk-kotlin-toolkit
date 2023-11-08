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
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.BuildConfig
import com.arcgismaps.toolkit.featureformsapp.data.network.ItemRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
) : ViewModel() {

    private val authenticatorState = AuthenticatorState()

    val loginState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            authenticatorState.pendingUsernamePasswordChallenge.collect {
                it?.continueWithCredentials(BuildConfig.webMapUser, BuildConfig.webMapPassword)
            }
        }
    }

    fun loginWithDefaultCredentials() {
        viewModelScope.launch(Dispatchers.IO) {
            authenticatorState.oAuthUserConfiguration = null
            val portal = Portal(ItemRemoteDataSource.portalUri, Portal.Connection.Authenticated)
            portal.load().onFailure {
                Log.e("TAG", "loginWithDefaultCredentials: $it", )
            }.onSuccess {
                val text = portal.portalInfo?.let {
                    val json = it.toJson()
                    val jsonObject = JSONObject(json)
                    jsonObject.toString(4)
                } ?: "no info"
                Log.e("TAG", "loginWithDefaultCredentials: Success $text", )
                loginState.value = true
            }
        }
    }
}

//val authenticationChallengeHandler = FormsArcGISAuthenticationChallengeHandler(
//    BuildConfig.webMapUser,
//    BuildConfig.webMapPassword
//)
//ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
//authenticationChallengeHandler

class FormsArcGISAuthenticationChallengeHandler(
    private val username: String,
    private val password: String
) : ArcGISAuthenticationChallengeHandler {
    override suspend fun handleArcGISAuthenticationChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val result: Result<TokenCredential> =
            TokenCredential.create(
                challenge.requestUrl,
                username,
                password,
                tokenExpirationInterval = 0
            )
        return result.let {
            if (it.isSuccess) {
                ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it.getOrThrow())
            } else {
                ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(it.exceptionOrNull()!!)
            }
        }
    }
}
