/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.mapviewidentifyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.toolkit.mapviewidentifyapp.screens.MainScreen
import com.arcgismaps.toolkit.mapviewidentifyapp.ui.theme.MapViewIdentifyAppTheme



class FeatureFormsTestChallengeHandler(
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            FeatureFormsTestChallengeHandler(
                BuildConfig.webMapUser,
                BuildConfig.webMapPassword
            )

//        ArcGISEnvironment.apiKey =
//            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            MapViewIdentifyAppTheme {
                MapViewIdentifyApp()
            }
        }
    }
}

@Composable
fun MapViewIdentifyApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MapViewIdentifyAppTheme {
        MapViewIdentifyApp()
    }
}
